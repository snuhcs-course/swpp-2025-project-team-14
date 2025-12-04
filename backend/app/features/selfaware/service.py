from __future__ import annotations

import random
from datetime import date

# from app.core.config import settings
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

from app.features.journal.repository import JournalRepository
from app.features.selfaware.models import Answer, Question, ValueMap
from app.features.selfaware.prompt import (
    MultiValueScoreStructure,
    OppositeValueStructure,
    ValueMapAnalysisStructure,
    get_opposite_value_prompt,
    value_map_combined_structured_prompt,
    value_score_structured_prompt,
)
from app.features.selfaware.repository import (
    AnswerRepository,
    QuestionRepository,
    ValueMapRepository,
    ValueScoreRepository,
)
from app.features.selfaware.strategy import (
    MultiStrategy,
    NatigationContext,
    SelfawareStrategy,
    SingleStrategy,
)

load_dotenv()


class QuestionService:
    def __init__(
        self,
        journal_repository: JournalRepository,
        question_repository: QuestionRepository,
    ) -> None:
        self.journal_repository = journal_repository
        self.question_repository = question_repository

    def generate_question(self, user_id: int) -> Question:
        navigation = NatigationContext()
        flag = random.randint(0, 2)
        if flag == 0 and self.journal_repository.list_journals_by_user(user_id, 1):
            navigation.set_question_strategy(SelfawareStrategy())
        elif flag == 1:
            navigation.set_question_strategy(SingleStrategy())
        else:
            navigation.set_question_strategy(MultiStrategy())
        return navigation.perform(
            user_id, self.journal_repository, self.question_repository
        )

    def get_questions_by_id(self, question_id: int) -> Question | None:
        return self.question_repository.get_question_by_id(question_id)

    def list_questions_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Question]:
        return self.question_repository.list_questions_by_user(user_id, limit, cursor)

    def get_questions_by_date(self, user_id: int, date: date) -> Question | None:
        return self.question_repository.get_question_by_date(user_id, date)

    def delete_question_by_id(self, question_id: int):
        return self.question_repository.delete_question_by_id(question_id)


class AnswerService:
    def __init__(
        self,
        answer_repository: AnswerRepository,
    ) -> None:
        self.answer_repository = answer_repository

    def create_answer(self, user_id: int, question_id: int, text: str) -> Answer:
        answer = self.answer_repository.create_answer(
            user_id=user_id, question_id=question_id, text=text
        )
        return answer

    def get_answer_by_question(self, question_id: int) -> Answer | None:
        return self.answer_repository.get_by_question(question_id)

    def get_answer_by_user(self, user_id: int) -> list[Answer]:
        return self.answer_repository.get_by_user(user_id)

    def list_answers_by_user(
        self, user_id: int, question_ids: list[int]
    ) -> list[Answer]:
        return self.answer_repository.list_answers_by_user(user_id, question_ids)

    def extract_keyword(self, id: int):
        pass


class ValueScoreService:
    def __init__(
        self,
        question_repository: QuestionRepository,
        answer_repository: AnswerRepository,
        value_score_repository: ValueScoreRepository,
        value_map_repository: ValueMapRepository,
    ) -> None:
        self.question_repository = question_repository
        self.answer_repository = answer_repository
        self.value_score_repository = value_score_repository
        self.value_map_repository = value_map_repository

    def extract_value_score_from_answer(
        self, user_id: int, question_id: int, answer_id: int
    ):
        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(
            MultiValueScoreStructure
        )

        question = self.question_repository.get_question_by_id(question_id)
        answer = self.answer_repository.get_answer_by_id(answer_id)

        if not question or not answer:
            raise Exception("question or answer not written")

        value_score_structured_chain = value_score_structured_prompt | llm

        response = value_score_structured_chain.invoke(
            {"question": question.text, "answer": answer.text}
        )

        assert isinstance(response, MultiValueScoreStructure)
        detected_values = response.detected_values

        # print("detected_values:", detected_values)

        # 혹은 value_map을 user가 등록되었을 때, craete해도 좋을 듯 합니다
        value_map = self.value_map_repository.get_by_user(user_id)
        if not value_map:
            print("value_map created")
            self.value_map_repository.create_value_map(user_id=user_id)

        for v in detected_values:
            value_score = self.value_score_repository.create_value_score(
                user_id=user_id,
                question_id=question_id,
                answer_id=answer_id,
                category=v.category_key,
                value=v.value,
                confidence=v.confidence,
                intensity=v.intensity,
                polarity=v.polarity,
                evidence_quotes=[v.evidence],
            )
            self.value_map_repository.update_by_value_score(value_score)

        return detected_values

    def get_top_value_scores(self, user_id: int):
        top_value_scores = self.value_score_repository.get_top_5_value_scores(user_id)
        value_scores = []

        print(top_value_scores)

        if top_value_scores:
            for top_value_score in top_value_scores:
                if top_value_score.polarity == 0:
                    continue  # polarity가 0이면 append하지 않음

                if top_value_score.polarity == -1:
                    llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(
                        OppositeValueStructure
                    )
                    get_opposite_value_chain = get_opposite_value_prompt | llm
                    response = get_opposite_value_chain.invoke(
                        {
                            "value": top_value_score.value,
                            "category": top_value_score.category,
                        }
                    )
                    value_scores.append(
                        {
                            "value": response.opposite_value,  # type: ignore
                            "intensity": top_value_score.intensity,
                        }
                    )
                else:
                    value_scores.append(
                        {
                            "value": top_value_score.value,
                            "intensity": top_value_score.intensity,
                        }
                    )

        return value_scores


class ValueMapService:
    def __init__(
        self,
        value_map_repository: ValueMapRepository,
        value_score_repository: ValueScoreRepository,
        answer_repository: AnswerRepository,
    ) -> None:
        self.value_score_repository = value_score_repository
        self.value_map_repository = value_map_repository
        self.answer_repository = answer_repository

    def create_value_map(self, user_id: int):
        return self.value_map_repository.create_value_map(user_id=user_id)

    def get_value_map_by_user(self, user_id) -> ValueMap | None:
        return self.value_map_repository.get_by_user(user_id)

    def generate_comment(self, user_id: int):
        value_map = self.value_map_repository.get_by_user(user_id)
        if not value_map:
            raise Exception("value_map does't exist")

        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(
            ValueMapAnalysisStructure
        )
        value_map_combined_structured_chain = value_map_combined_structured_prompt | llm

        response = value_map_combined_structured_chain.invoke(
            {
                "score_0": value_map.score_0,
                "score_1": value_map.score_1,
                "score_2": value_map.score_2,
                "score_3": value_map.score_3,
                "score_4": value_map.score_4,
            }
        )
        assert isinstance(response, ValueMapAnalysisStructure)

        return self.value_map_repository.generate_comment(
            user_id=user_id,
            personality_insight=response.personality_insight,
            comment=response.comment,
        )
