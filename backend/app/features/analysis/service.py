from __future__ import annotations
from typing import Annotated, List, Optional, Literal, Dict, Any, Tuple
from datetime import datetime, timezone, date
import random
from app.core.config import settings
from dotenv import load_dotenv

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser, PydanticOutputParser

from app.features.journal.models import Journal
from app.features.journal.repository import JournalRepository

from app.features.selfaware.models import (
    Question, 
    Answer, 
    ValueMap, 
    ValueScore
)
from app.features.selfaware.prompt import (
    category_prompt,
    personalized_prompt, 
    single_category_prompt, 
    multi_category_prompt, 
    value_score_structured_prompt, 
    value_map_combined_structured_prompt, 
    get_opposite_value_prompt
)
from app.features.selfaware.prompt import (
    MultiValueScoreStructure, 
    ValueMapAnalysisStructure, 
    OppositeValueStructure, 
    JournalSummary
)
from app.features.selfaware.repository import (
    QuestionRepository, 
    AnswerRepository, 
    ValueMapRepository, 
    ValueScoreRepository
)
from app.features.selfaware.prompt import (
    CATEGORIES, 
    CategoryExtractionResponse, 
    QuestionGenerationResponse
)

from app.features.selfaware.personality_insight.score import (
    questions, choices, prompt, 
    NeoPiAnswers
)

from app.features.selfaware.personality_insight.evaluator import evaluate

from app.features.selfaware.personality_insight.data.en.prompts import (
    big_5_prompt, 
    total_comment_prompt, 
    extraversion_explanations, 
    openness_explanations, 
    agreeableness_explanations, 
    conscientiousness_explanations, 
    neuroticism_explanations
)

from app.features.analysis.models import Analysis
from app.features.analysis.repository import AnalysisRepository
from app.features.analysis.prompt import personalized_advice_prompt

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
    
    def extract_neo_pi_from_answer(self, user_id:int):
        llm = ChatOpenAI(model="gpt-5-nano").with_structured_output(NeoPiAnswers)

        answers = self.answer_repository.get_by_user(user_id)

        if not answers:
            raise Exception("answer has not written")
        
        answers_text = [answer.text for answer in answers]

        neo_pi_questions = questions
        neo_pi_choices   = choices
        neo_pi_prompt    = prompt

        neo_pi_chain = neo_pi_prompt | llm
        
        responses = [neo_pi_chain.invoke({
            "choices": neo_pi_choices,
            "questions": neo_pi_questions[20*i:20*(i+1)],
            "conversation": answers_text,
        }) for i in range(6)]

        assert type(responses[0]) == NeoPiAnswers
        assert type(responses[1]) == NeoPiAnswers
        assert type(responses[2]) == NeoPiAnswers
        assert type(responses[3]) == NeoPiAnswers
        assert type(responses[4]) == NeoPiAnswers
        assert type(responses[5]) == NeoPiAnswers

        if len(responses[0].answers) != 20:
            raise
        if len(responses[1].answers) != 20:
            raise   
        if len(responses[2].answers) != 20:
            raise   
        if len(responses[3].answers) != 20:
            raise   
        if len(responses[4].answers) != 20:
            raise   
        if len(responses[5].answers) != 20:
            raise       
            
        total_response = [0] + responses[0].answers + responses[1].answers + responses[2].answers + responses[3].answers + responses[4].answers + responses[5].answers
        print("valid big 5 score generated")
        return total_response
    
    def evaluate_big_5_score(self, user_id, age, sex, flag = False):
        neo_pi = self.extract_neo_pi_from_answer(user_id)
        return evaluate(neo_pi, sex, age, flag)
    
    def update_neo_pi_score(self, user_id: int):
        neo_pi_score = self.evaluate_big_5_score(user_id, 23, "Male", flag = True)
        self.analysis_repository.update_analysis(user_id=user_id, neo_pi_score=neo_pi_score)
    
    def evaluate_user_type(self, user_id):
        analysis = self.analysis_repository.get_analysis_by_user_id(user_id)
        if analysis == None:
            raise
        score = analysis.neo_pi_score
        if score == None:
            raise
        C, N, E, O, A = score['CONSCIENTIOUSNESS'], score['NEUROTICISM'], score['EXTRAVERSION'], score['OPENNESS'], score['AGREEABLENESS']
        if C >= 65 and N <= 45:
            return "목표 지향형"
        if O >= 60 and E >= 60:
            return "탐험가형"
        if E >= 60 and A >= 60:
            return "사교가형"
        if C >= 60 and A >= 60:
            return "배려형"
        if O >= 60 and E <= 45:
            return "사색가형"
        if E >= 60 and A <= 45:
            return "도전형"
        if N <= 40 and C >= 50:
            return "안정추구형"
        if N >= 60 and O >= 60:
            return "감성형"
        if C >= 65 and O <= 45:
            return "분석형"
        if O >= 65 and C <= 45:
            return "변화추구형"
        return "균형형"
    
    def update_user_type(self, user_id: int):
        user_type = self.evaluate_user_type(user_id)
        self.analysis_repository.update_analysis(user_id=user_id, user_type=user_type)

    def get_comment_from_big_5_score(self, user_id, age, sex):
        score_json = self.evaluate_big_5_score(user_id, age, sex)
        print(score_json)
        parser = StrOutputParser()
        llm = ChatOpenAI(model="gpt-5-nano")
        big_5_chain = big_5_prompt | llm | parser
        total_comment_chain = total_comment_prompt | llm | parser
        a_response = big_5_chain.invoke({"big_5_explanations": agreeableness_explanations, "big_5_score": score_json})
        c_response = big_5_chain.invoke({"big_5_explanations": conscientiousness_explanations, "big_5_score": score_json})
        e_response = big_5_chain.invoke({"big_5_explanations": extraversion_explanations, "big_5_score": score_json})
        n_response = big_5_chain.invoke({"big_5_explanations": neuroticism_explanations, "big_5_score": score_json})
        o_response = big_5_chain.invoke({"big_5_explanations": openness_explanations, "big_5_score": score_json})
        total_response = total_comment_chain.invoke({"a_response": a_response,"c_response": c_response,"e_response": e_response,"n_response": n_response,"o_response": o_response})
        # example output
        # 당신은 타인에 대한 신뢰와 공감을 바탕으로 협력과 조화를 중시하는 성향이 강하고, 갈등을 피하며 상황에 따라 타협하는 균형 감각이 돋보입니다. 필요할 때는 진실되고 분명하게 자기주장을 표현하는 편이라, 다른 사람의 필요를 돕는 동시에 자신의 목소리도 잃지 않는 모습을 보입니다. 계획성과 체계성을 바탕으로 목표를 명확히 설정하고 꾸준히 달성하려는 자기효능감과 의무감이 강하며, 질서정돈과 성취추구, 자기절제의 특성이 뚜렷합니다. 즉흥적 선택보다는 신중한 판단을 우선하고, 어려운 과제도 끝까지 밀고 나가려는 지속성을 가지며, 상황에 맞춘 주의 집중력을 발휘합니다. 외향적이고 에너지 넘치는 사회적 성향으로 사람들과의 교류에서 활력을 얻되, 자신의 페이스를 지키는 여유도 함께 갖추고 있습니다. 미학과 아이디어에 대한 예민한 감수성과 창 의적 사고를 통해 추상적이고 상징적인 표현을 즐기고, 새로운 경험과 규범에 대한 도전 의식이 강합니다. 그러나 타인 평가에 예민하고 스트레스 상황에서 일시적으로 흔들릴 수 있어, 스트레 스 관리와 감정 조절이 도움이 될 수 있습니다. 전반적으로 구조화된 환경에서도 창의성과 대인 관계를 균형 있게 발휘하는 능력이 돋보이며, 과도한 완벽주의나 몰입으로 인한 부작용을 주의하면 더욱 생산성과 창의성을 폭넓게 확장할 수 있습니다.
        return total_response
    
    def update_comprehensive_analysis(self, user_id: int):
        comprehensive_analysis = self.get_comment_from_big_5_score(user_id, 23, "Male")
        self.analysis_repository.update_analysis(user_id=user_id, comprehensive_analysis=comprehensive_analysis)

    def extract_personalized_advice(self, user_id: int, age, sex):
        analysis = self.analysis_repository.get_analysis_by_user_id(user_id)
        if analysis == None:
            raise
        score = analysis.neo_pi_score
        if score == None:
            raise

        llm = ChatOpenAI(model="gpt-5-nano")
        personalized_advice_chain = personalized_advice_prompt | llm | StrOutputParser()

        theory = random.choice(["CBT", "ACT", "EQ"])

        response = personalized_advice_chain.invoke({"theory": theory, "neo_pi_summary": score})

        return response
    
    def update_personalized_advice(self, user_id: int):
        personalized_advice = self.extract_personalized_advice(user_id, 23, "Male")
        self.analysis_repository.update_analysis(user_id=user_id, personalized_advice=personalized_advice)