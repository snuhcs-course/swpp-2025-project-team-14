import pytest
from unittest.mock import Mock, MagicMock, patch
from datetime import date
from typing import List, Tuple
import json

# 테스트할 서비스 클래스들을 임포트합니다.
from app.features.selfaware.prompt import CATEGORIES
from app.features.selfaware.service import QuestionService, AnswerService, ValueScoreService, ValueMapService 
from app.features.selfaware.prompt import JournalSummary, CategoryExtractionResponse, QuestionGenerationResponse, MultiValueScoreStructure, ValueScoreStructure as DetectedValue, OppositeValueStructure, ValueMapAnalysisStructure

class MockAIMessage:
    # LangChain BaseMessage의 content 속성을 모방
    def __init__(self, content):
        self.content = content
    # LangChain이 Generation 객체를 생성할 때 사용되는 text 추출을 위해 
    # content 속성을 가진 Mock 객체가 필요합니다.
    def __str__(self):
        return self.content

# DB 모델 및 구조체 Mock
class MockQuestion:
    def __init__(self, id, user_id, question_type, text, created_at=None):
        self.id = id
        self.user_id = user_id
        self.question_type = question_type
        self.text = text
        self.created_at = created_at if created_at else date.today()

class MockAnswer:
    def __init__(self, id, user_id, question_id, text):
        self.id = id
        self.user_id = user_id
        self.question_id = question_id
        self.text = text

class MockJournal:
    def __init__(self, id, user_id, content, created_at):
        self.id = id
        self.user_id = user_id
        self.content = content
        self.created_at = created_at

class MockValueScore:
    def __init__(self, id, user_id, value, category, intensity, polarity, confidence, evidence_quotes):
        self.id = id
        self.user_id = user_id
        self.value = value
        self.category = category
        self.intensity = intensity
        self.polarity = polarity
        self.confidence = confidence
        self.evidence_quotes = evidence_quotes

class MockValueMap:
    def __init__(self, user_id, score_0=0, score_1=0, score_2=0, score_3=0, score_4=0, comment="", personality_insight=""):
        self.user_id = user_id
        self.score_0 = score_0
        self.score_1 = score_1
        self.score_2 = score_2
        self.score_3 = score_3
        self.score_4 = score_4
        self.comment = comment
        self.personality_insight = personality_insight

# QuestionService 테스트
@patch('app.features.selfaware.service.ChatOpenAI') # ChatOpenAI를 Mocking하여 실제 LLM 호출을 방지합니다.
class TestQuestionService:
    @pytest.fixture
    def setup_service(self):
        # Repository Mock 객체 생성
        self.mock_journal_repo = Mock()
        self.mock_question_repo = Mock()
        
        # Service 인스턴스 생성
        self.question_service = QuestionService(
            journal_repository=self.mock_journal_repo,
            question_repository=self.mock_question_repo,
        )
        self.USER_ID = 1

    def test_generate_selfaware_question_with_journals(self, MockChatOpenAI, setup_service):
        """일기가 있을 때 개인화된 질문을 생성하는 로직을 테스트합니다."""
        
        # 1. Mock 데이터 설정
        recent_journals = [
            MockJournal(i, self.USER_ID, f"일기 내용 {i}", date.today()) for i in range(1, 6)
        ]
        self.mock_journal_repo.list_journals_by_user.return_value = recent_journals

        # LLM 응답 Mock 설정
        mock_llm_instance = MagicMock()
        # with_structured_output이 자기 자신을 반환하도록 설정
        mock_llm_instance.with_structured_output.return_value = mock_llm_instance
        MockChatOpenAI.return_value = mock_llm_instance

        # Mocking된 AIMessage 객체를 생성하는 헬퍼 함수
        def create_mock_message(pydantic_instance):
            # Pydantic 객체를 JSON 문자열로 변환 (실제 LLM 출력 모방)
            # Pydantic 2.x에서는 model_dump_json() 사용
            try:
                content_str = pydantic_instance.model_dump_json()
            except AttributeError:
                # Pydantic 1.x 호환 (dump_json 대신 json() 사용)
                content_str = pydantic_instance.json()
            
            # AIMessage Mock 생성
            mock_message = Mock(spec=MockAIMessage)
            mock_message.content = content_str
            return mock_message

        # Chain.invoke 결과 Mocking: LLM은 AIMessage 객체를 반환합니다.
        mock_llm_instance.invoke.side_effect = [
            # 1. Summary Response
            create_mock_message(JournalSummary(summary="최근 일기는 업무 스트레스와 가족에 대한 걱정을 주로 다루고 있습니다.")),
            
            # 2. Category Response
            create_mock_message(
                CategoryExtractionResponse(
                    analysis="사용자는 스트레스로 인해 가족과의 관계에서 불편함을 느끼고 있습니다.",
                    categories=[("Work", "직장"), ("Family", "가족")]
                )
            ),
            
            # 3. Question Response
            create_mock_message(
                QuestionGenerationResponse(question="최근 업무 스트레스가 가족과의 상호작용에 어떤 영향을 미쳤나요?", rationale="분석 결과를 바탕으로 핵심 갈등을 질문으로 만듦")
            ),
        ]

        # Question 생성 Mock
        expected_question = MockQuestion(10, self.USER_ID, "personalized_category", "최근 업무 스트레스가 가족과의 상호작용에 어떤 영향을 미쳤나요?")
        self.mock_question_repo.create_question.return_value = expected_question

        # 2. 함수 호출
        question = self.question_service.generate_selfaware_question(self.USER_ID)

        # 3. 검증
        assert question == expected_question
        self.mock_journal_repo.list_journals_by_user.assert_called_once_with(self.USER_ID)
        # LLM 호출이 3번 일어났는지 확인
        assert mock_llm_instance.invoke.call_count == 3
        # Question Repository에 저장되었는지 확인
        self.mock_question_repo.create_question.assert_called_once_with(
            user_id=self.USER_ID,
            question_type="personalized_category",
            text="최근 업무 스트레스가 가족과의 상호작용에 어떤 영향을 미쳤나요?",
        )

    @patch('app.features.selfaware.service.random.randint', return_value=0) # 일기가 없을 때 single_category_question을 호출하도록 Mocking
    def test_generate_selfaware_question_no_journals_single(self, mock_randint, MockChatOpenAI, setup_service):
        """일기가 없을 때 단일 카테고리 질문 생성 로직을 호출하는지 테스트합니다."""
        self.mock_journal_repo.list_journals_by_user.return_value = []
        
        # single_category_question 내부 로직이 실제로 실행되지 않고, 외부에서 Mocking된 Question이 반환되도록 설정
        self.question_service.generate_single_category_question = Mock(return_value=MockQuestion(11, self.USER_ID, "single_category", "랜덤 단일 질문"))
        
        question = self.question_service.generate_selfaware_question(self.USER_ID)

        assert question.question_type == "single_category"
        self.question_service.generate_single_category_question.assert_called_once_with(self.USER_ID)
        self.mock_journal_repo.list_journals_by_user.assert_called_once_with(self.USER_ID)
        mock_randint.assert_called_once_with(0, 1)

    @patch('app.features.selfaware.service.random.choice', return_value=("Growth", "성장")) # 고정된 카테고리 선택 Mock
    def test_generate_single_category_question(self, mock_choice, MockChatOpenAI, setup_service):
        """단일 카테고리 질문 생성 로직을 테스트합니다."""
        mock_llm_instance = MagicMock()
        mock_llm_instance.with_structured_output.return_value = mock_llm_instance
        MockChatOpenAI.return_value = mock_llm_instance
        
        # LLM 응답 Mock 설정
        mock_llm_instance.invoke.return_value = QuestionGenerationResponse(
            question="성장 영역에서 최근 스스로 자랑스러웠던 순간은 언제였나요?", rationale=""
        )
        
        expected_question = MockQuestion(12, self.USER_ID, "single_category", "성장 영역에서 최근 스스로 자랑스러웠던 순간은 언제였나요?")
        self.mock_question_repo.create_question.return_value = expected_question
        
        question = self.question_service.generate_single_category_question(self.USER_ID)
        
        assert question == expected_question
        assert "Growth(성장)" in mock_llm_instance.invoke.call_args[1]['category']
        self.mock_question_repo.create_question.assert_called_once_with(
            user_id=self.USER_ID,
            question_type="single_category",
            text="성장 영역에서 최근 스스로 자랑스러웠던 순간은 언제였나요?",
        )

    @patch('app.features.selfaware.service.random.randint', side_effect=[2, 2]) # 2개 카테고리 선택, generate_question에서 type=2 선택
    @patch('app.features.selfaware.service.random.sample', return_value=[("Health", "건강"), ("Hobby", "취미")]) # 고정된 카테고리 선택 Mock
    def test_generate_multi_category_question(self, mock_sample, mock_randint, MockChatOpenAI, setup_service):
        """다중 카테고리 질문 생성 로직을 테스트합니다."""
        mock_llm_instance = MagicMock()
        mock_llm_instance.with_structured_output.return_value = mock_llm_instance
        MockChatOpenAI.return_value = mock_llm_instance
        
        # LLM 응답 Mock 설정
        mock_llm_instance.invoke.return_value = QuestionGenerationResponse(
            question="최근 취미 활동이 신체적 건강에 어떤 긍정적인 영향을 주고 있나요?", rationale=""
        )
        
        expected_question = MockQuestion(13, self.USER_ID, "multi_category", "최근 취미 활동이 신체적 건강에 어떤 긍정적인 영향을 주고 있나요?")
        self.mock_question_repo.create_question.return_value = expected_question
        
        question = self.question_service.generate_multi_category_question(self.USER_ID)
        
        assert question == expected_question
        # "Health(건강), Hobby(취미)"가 포함되어야 함
        categories_arg = mock_llm_instance.invoke.call_args[1]['categories']
        assert "Health(건강)" in categories_arg and "Hobby(취미)" in categories_arg
        self.mock_question_repo.create_question.assert_called_once_with(
            user_id=self.USER_ID,
            question_type="multi_category",
            text="최근 취미 활동이 신체적 건강에 어떤 긍정적인 영향을 주고 있나요?",
        )

# AnswerService 테스트
class TestAnswerService:
    @pytest.fixture
    def setup_service(self):
        self.mock_answer_repo = Mock()
        self.answer_service = AnswerService(answer_repository=self.mock_answer_repo)
        self.USER_ID = 1
        self.QUESTION_ID = 5
        self.ANSWER_TEXT = "질문에 대한 답변입니다."

    def test_create_answer(self, setup_service):
        """답변 생성 로직을 테스트합니다."""
        expected_answer = MockAnswer(1, self.USER_ID, self.QUESTION_ID, self.ANSWER_TEXT)
        self.mock_answer_repo.create_answer.return_value = expected_answer
        
        answer = self.answer_service.create_answer(self.USER_ID, self.QUESTION_ID, self.ANSWER_TEXT)
        
        assert answer == expected_answer
        self.mock_answer_repo.create_answer.assert_called_once_with(
            user_id=self.USER_ID,
            question_id=self.QUESTION_ID,
            text=self.ANSWER_TEXT
        )

# ValueScoreService 테스트
@patch('app.features.selfaware.service.ChatOpenAI')
class TestValueScoreService:
    @pytest.fixture
    def setup_service(self):
        self.mock_question_repo = Mock()
        self.mock_answer_repo = Mock()
        self.mock_value_score_repo = Mock()
        self.mock_value_map_repo = Mock()
        
        self.value_score_service = ValueScoreService(
            question_repository=self.mock_question_repo,
            answer_repository=self.mock_answer_repo,
            value_score_repository=self.mock_value_score_repo,
            value_map_repository=self.mock_value_map_repo,
        )
        self.USER_ID = 1
        self.QUESTION_ID = 5
        self.ANSWER_ID = 1

    def test_extract_value_score_from_answer_success(self, MockChatOpenAI, setup_service):
        """답변에서 가치 점수를 추출하고 DB에 저장하는 로직을 테스트합니다."""
        
        # 1. Mock 데이터 설정
        mock_question = MockQuestion(self.QUESTION_ID, self.USER_ID, "single_category", "오늘의 질문")
        mock_answer = MockAnswer(self.ANSWER_ID, self.USER_ID, self.QUESTION_ID, "오늘의 답변")
        self.mock_question_repo.get_question_by_id.return_value = mock_question
        self.mock_answer_repo.get_answer_by_id.return_value = mock_answer
        self.mock_value_map_repo.get_by_user.return_value = None # ValueMap이 없어서 새로 생성하는 경우를 테스트

        # LLM 응답 Mock 설정
        detected_value_1 = DetectedValue(
            category_key="Growth", value="성장", confidence=0.9, intensity=3, polarity=1, evidence="스스로 자랑스러웠다"
        )
        detected_value_2 = DetectedValue(
            category_key="Family", value="가족", confidence=0.7, intensity=2, polarity=1, evidence="가족과 좋은 시간"
        )
        llm_response = MultiValueScoreStructure(detected_values=[detected_value_1, detected_value_2])
        
        mock_llm_instance = MagicMock()
        MockChatOpenAI.return_value = mock_llm_instance
        mock_llm_instance.with_structured_output.return_value = mock_llm_instance
        mock_llm_instance.invoke.return_value = llm_response

        # ValueScore 생성 Mock
        mock_value_score = MockValueScore(1, self.USER_ID, "성장", "Growth", 3, 1, 0.9, ["스스로 자랑스러웠다"])
        self.mock_value_score_repo.create_value_score.return_value = mock_value_score

        # 2. 함수 호출
        detected_values = self.value_score_service.extract_value_score_from_answer(
            self.USER_ID, self.QUESTION_ID, self.ANSWER_ID
        )

        # 3. 검증
        assert len(detected_values) == 2
        # ValueMap 생성 호출 확인 (없었으므로)
        self.mock_value_map_repo.create_value_map.assert_called_once_with(user_id=self.USER_ID)
        # ValueScore 생성 호출 확인 (2번)
        assert self.mock_value_score_repo.create_value_score.call_count == 2
        # ValueMap 업데이트 호출 확인 (2번)
        assert self.mock_value_map_repo.update_by_value_score.call_count == 2

    def test_get_top_value_scores_with_negative_polarity(self, MockChatOpenAI, setup_service):
        """음성 극성(-1) 가치 점수에 대해 반대 가치를 추출하는 로직을 테스트합니다."""
        
        # 1. Mock 데이터 설정
        # 양성 가치
        positive_score = MockValueScore(1, self.USER_ID, "행복", "Emotion", 5, 1, 1.0, [])
        # 음성 가치 -> 반대 가치로 변환되어야 함
        negative_score = MockValueScore(2, self.USER_ID, "불안", "Emotion", 4, -1, 0.8, [])
        # 중립 가치 -> 제외되어야 함
        neutral_score = MockValueScore(3, self.USER_ID, "중립", "Neutral", 1, 0, 0.5, [])
        
        self.mock_value_score_repo.get_top_5_value_scores.return_value = [positive_score, negative_score, neutral_score]

        # LLM 응답 Mock 설정 (음성 가치에 대한 반대 가치 추출)
        mock_llm_instance = MagicMock()
        MockChatOpenAI.return_value = mock_llm_instance
        # -1 극성일 때만 with_structured_output이 호출되므로, 그 때의 invoke 결과만 Mocking
        mock_llm_instance.with_structured_output.return_value = mock_llm_instance
        mock_llm_instance.invoke.return_value = OppositeValueStructure(opposite_value="평온")

        # 2. 함수 호출
        top_scores = self.value_score_service.get_top_value_scores(self.USER_ID)

        # 3. 검증
        # 중립(polarity=0)은 제외되어 2개만 반환되어야 함
        assert len(top_scores) == 2
        
        # 양성 가치
        assert top_scores[0]['value'] == "행복"
        assert top_scores[0]['intensity'] == 5
        
        # 음성 가치가 반대 가치 "평온"으로 변환되었는지 확인
        assert top_scores[1]['value'] == "평온"
        assert top_scores[1]['intensity'] == 4
        
        # get_opposite_value_prompt 호출 확인
        mock_llm_instance.invoke.assert_called_once()
        assert mock_llm_instance.invoke.call_args[1]['value'] == "불안"

# ValueMapService 테스트
@patch('app.features.selfaware.service.ChatOpenAI')
class TestValueMapService:
    @pytest.fixture
    def setup_service(self):
        self.mock_value_map_repo = Mock()
        self.mock_value_score_repo = Mock()
        self.mock_answer_repo = Mock()
        
        self.value_map_service = ValueMapService(
            value_map_repository=self.mock_value_map_repo,
            value_score_repository=self.mock_value_score_repo,
            answer_repository=self.mock_answer_repo,
        )
        self.USER_ID = 1

    def test_generate_comment_success(self, MockChatOpenAI, setup_service):
        """ValueMap 점수를 기반으로 LLM이 코멘트를 생성하고 DB에 업데이트하는 로직을 테스트합니다."""
        
        # 1. Mock 데이터 설정
        mock_value_map = MockValueMap(
            user_id=self.USER_ID, 
            score_0=10, # Category 0 점수
            score_1=5,
            score_2=8,
            score_3=3,
            score_4=12  # Category 4 점수
        )
        self.mock_value_map_repo.get_by_user.return_value = mock_value_map

        # LLM 응답 Mock 설정
        llm_response = ValueMapAnalysisStructure(
            personality_insight="가족과 성장에 대한 가치 점수가 높습니다.",
            comment="당신은 가족과의 유대와 개인적인 발전을 중요하게 생각합니다. 이러한 가치를 기반으로 ...",
        )
        
        mock_llm_instance = MagicMock()
        MockChatOpenAI.return_value = mock_llm_instance
        mock_llm_instance.with_structured_output.return_value = mock_llm_instance
        mock_llm_instance.invoke.return_value = llm_response

        # DB 업데이트 Mock
        expected_value_map = MockValueMap(
            user_id=self.USER_ID, 
            score_0=10, score_1=5, score_2=8, score_3=3, score_4=12,
            comment=llm_response.comment, 
            personality_insight=llm_response.personality_insight
        )
        self.mock_value_map_repo.generate_comment.return_value = expected_value_map

        # 2. 함수 호출
        updated_value_map = self.value_map_service.generate_comment(self.USER_ID)

        # 3. 검증
        assert updated_value_map == expected_value_map
        # LLM 호출 시 ValueMap 점수가 제대로 전달되었는지 확인
        invoke_args = mock_llm_instance.invoke.call_args[0][0]
        assert invoke_args['score_0'] == 10
        assert invoke_args['score_4'] == 12
        # DB 업데이트 호출 확인
        self.mock_value_map_repo.generate_comment.assert_called_once_with(
            user_id=self.USER_ID,
            personality_insight=llm_response.personality_insight,
            comment=llm_response.comment
        )