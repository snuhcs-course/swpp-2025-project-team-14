from __future__ import annotations

import json
import random

from dotenv import load_dotenv
from langchain_core.output_parsers import PydanticOutputParser, StrOutputParser
from langchain_openai import ChatOpenAI

from app.features.analysis.comprehensive_analysis.data.en.prompts import (
    agreeableness_explanations,
    big_5_prompt,
    conscientiousness_explanations,
    extraversion_explanations,
    neuroticism_explanations,
    openness_explanations,
)
from app.features.analysis.comprehensive_analysis.evaluator import evaluate
from app.features.analysis.comprehensive_analysis.score import (
    NeoPiAnswers,
    choices,
    prompt,
    questions,
)
from app.features.analysis.prompt import (
    AdviceGenerationResponse,
    personalized_advice_prompt,
)
from app.features.analysis.repository import AnalysisRepository
from app.features.selfaware.repository import AnswerRepository

load_dotenv()


class AnalysisService:
    def __init__(
        self,
        answer_repository: AnswerRepository,
        analysis_repository: AnalysisRepository,
    ) -> None:
        self.answer_repository = answer_repository
        self.analysis_repository = analysis_repository

    def create_analysis(self, user_id: int):
        return self.analysis_repository.create_analysis(user_id=user_id)

    def get_analysis_by_user(self, user_id: int):
        return self.analysis_repository.get_analysis_by_user_id(user_id=user_id)

    def extract_neo_pi_from_answer(self, user_id: int):
        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(NeoPiAnswers)

        answers = self.answer_repository.get_by_user(user_id)

        if not answers:
            raise Exception("answer has not written")

        answers_text = [answer.text for answer in answers]

        neo_pi_questions = questions
        neo_pi_choices = choices
        neo_pi_prompt = prompt

        neo_pi_chain = neo_pi_prompt | llm

        responses = [
            neo_pi_chain.invoke(
                {
                    "choices": neo_pi_choices,
                    "questions": neo_pi_questions[20 * i : 20 * (i + 1)],
                    "conversation": answers_text,
                }
            )
            for i in range(6)
        ]

        for response in responses:
            assert isinstance(response, NeoPiAnswers)
            if len(response.answers) != 20:
                raise ValueError("Each NEO PI answer set must contain 20 answers.")

        total_response = (
            [0]
            + responses[0].answers
            + responses[1].answers
            + responses[2].answers
            + responses[3].answers
            + responses[4].answers
            + responses[5].answers
        )
        print("valid big 5 score generated")
        return total_response

    def evaluate_big_5_score(self, user_id, age, gender, flag=False):
        neo_pi = self.extract_neo_pi_from_answer(user_id)
        return evaluate(neo_pi, gender, age, flag)

    def update_neo_pi_score(self, user_id: int, age: int = 23, gender: str = "Male"):
        neo_pi_score = self.evaluate_big_5_score(user_id, age, gender, flag=False)
        self.analysis_repository.update_analysis(
            user_id=user_id, neo_pi_score=neo_pi_score
        )

    def evaluate_user_type(self, user_id):
        analysis = self.analysis_repository.get_analysis_by_user_id(user_id)
        if analysis is None:
            raise ValueError("Analysis not found for user.")
        score = analysis.neo_pi_score
        if score is None:
            raise ValueError("NEO PI score not found for user.")
        C, N, E, O, A = (  # noqa: E741, N806
            score["CONSCIENTIOUSNESS"],
            score["NEUROTICISM"],
            score["EXTRAVERSION"],
            score["OPENNESS"],
            score["AGREEABLENESS"],
        )
        if C >= 65 and N <= 45:
            return "Goal oriented"
        if O >= 60 and E >= 60:
            return "Adventurer"
        if E >= 60 and A >= 60:
            return "Connector"
        if C >= 60 and A >= 60:
            return "Supportive achiever"
        if O >= 60 and E <= 45:
            return "Explorer"
        if E >= 60 and A <= 45:
            return "Challenger"
        if N <= 40 and C >= 50:
            return "Stability seeker"
        if N >= 60 and O >= 60:
            return "Sensitive creator"
        if C >= 65 and O <= 45:
            return "Analyst"
        if O >= 65 and C <= 45:
            return "Change seeker"
        return "Balanced"

    def update_user_type(self, user_id: int):
        user_type = self.evaluate_user_type(user_id)
        self.analysis_repository.update_analysis(user_id=user_id, user_type=user_type)

    def get_comment_from_big_5_score(self, user_id, age, gender):
        analysis = self.get_analysis_by_user(user_id)
        if analysis is None or analysis.neo_pi_score is None:
            raise ValueError("NEO PI score not found for user.")
        score_json = json.dumps(analysis.neo_pi_score, indent=4)
        parser = StrOutputParser()
        llm = ChatOpenAI(model="gpt-5-nano")
        big_5_chain = big_5_prompt | llm | parser
        a_response = big_5_chain.invoke(
            {
                "big_5_explanations": agreeableness_explanations,
                "big_5_score": score_json,
            }
        )
        c_response = big_5_chain.invoke(
            {
                "big_5_explanations": conscientiousness_explanations,
                "big_5_score": score_json,
            }
        )
        e_response = big_5_chain.invoke(
            {"big_5_explanations": extraversion_explanations, "big_5_score": score_json}
        )
        n_response = big_5_chain.invoke(
            {"big_5_explanations": neuroticism_explanations, "big_5_score": score_json}
        )
        o_response = big_5_chain.invoke(
            {"big_5_explanations": openness_explanations, "big_5_score": score_json}
        )
        return a_response, c_response, e_response, n_response, o_response

    def update_comprehensive_analysis(
        self, user_id: int, age: int = 23, gender: str = "Male"
    ):
        a_response, c_response, e_response, n_response, o_response = (
            self.get_comment_from_big_5_score(user_id, age, gender)
        )
        self.analysis_repository.update_analysis(
            user_id=user_id,
            conscientiousness=c_response,
            neuroticism=n_response,
            extraversion=e_response,
            openness=o_response,
            agreeableness=a_response,
        )

    def extract_personalized_advice(self, user_id: int, age, gender):
        analysis = self.analysis_repository.get_analysis_by_user_id(user_id)
        if analysis is None:
            raise ValueError("Analysis not found for user.")
        score = analysis.neo_pi_score
        if score is None:
            raise ValueError("NEO PI score not found for user.")

        llm = ChatOpenAI(model="gpt-5-nano")
        output_parser = PydanticOutputParser(pydantic_object=AdviceGenerationResponse)
        personalized_advice_chain = personalized_advice_prompt | llm | output_parser

        theory = random.choice(["CBT", "ACT", "EQ"])

        response = personalized_advice_chain.invoke(
            {"theory": theory, "neo_pi_summary": score}
        )

        return response

    def update_personalized_advice(
        self, user_id: int, age: int = 23, gender: str = "Male"
    ):
        response = self.extract_personalized_advice(user_id, age, gender)
        self.analysis_repository.update_analysis(
            user_id=user_id,
            advice_type=response.theory,
            personalized_advice=response.advice,
        )
