# backend/tests/unit/analysis/test_service.py
import pytest
from app.features.analysis.service import AnalysisService


# ------------------------------
# 공통 fixture
# ------------------------------
@pytest.fixture
def mock_answer_repo(mocker):
    repo = mocker.Mock()
    repo.get_by_user.return_value = [mocker.Mock(text="오늘은 행복했어요.")]
    return repo


@pytest.fixture
def mock_analysis_repo(mocker):
    repo = mocker.Mock()
    repo.create_analysis.return_value = {"id": 1, "user_id": 1}
    repo.get_analysis_by_user_id.return_value = mocker.Mock(
        neo_pi_score={
            "CONSCIENTIOUSNESS": 70,
            "NEUROTICISM": 40,
            "EXTRAVERSION": 65,
            "OPENNESS": 60,
            "AGREEABLENESS": 58,
        }
    )
    return repo


@pytest.fixture
def service(mock_answer_repo, mock_analysis_repo):
    return AnalysisService(answer_repository=mock_answer_repo, analysis_repository=mock_analysis_repo)


# ------------------------------
# 1️⃣ 기본 CRUD 테스트
# ------------------------------
def test_create_and_get_analysis(service, mock_analysis_repo):
    created = service.create_analysis(1)
    fetched = service.get_analysis_by_user(1)

    mock_analysis_repo.create_analysis.assert_called_once_with(user_id=1)
    mock_analysis_repo.get_analysis_by_user_id.assert_called_once_with(user_id=1)
    assert "id" in created
    assert fetched.neo_pi_score["CONSCIENTIOUSNESS"] == 70


# ------------------------------
# 2️⃣ 사용자 유형 평가 로직
# ------------------------------
def test_evaluate_user_type_returns_expected(service, mock_analysis_repo):
    user_type = service.evaluate_user_type(user_id=1)
    assert user_type == "목표 지향형"  # C >=65, N <=45


# ------------------------------
# 3️⃣ extract_neo_pi_from_answer 테스트 (LLM 모의)
# ------------------------------
def test_extract_neo_pi_from_answer_mocked_llm(service, mocker, mock_answer_repo):
    mock_llm = mocker.Mock()
    mock_llm.with_structured_output.return_value = mock_llm
    mock_llm.invoke.return_value = mocker.Mock(answers=[1] * 20)
    mocker.patch("app.features.analysis.service.ChatOpenAI", return_value=mock_llm)

    responses = service.extract_neo_pi_from_answer(user_id=1)
    assert isinstance(responses, list)
    assert len(responses) > 0


# ------------------------------
# 4️⃣ Big5 점수 평가 로직 (evaluate 함수 mock)
# ------------------------------
def test_evaluate_big_5_score(service, mocker):
    mocker.patch.object(service, "extract_neo_pi_from_answer", return_value=[1] * 121)
    mock_evaluate = mocker.patch("app.features.analysis.service.evaluate", return_value={"O": 60})
    result = service.evaluate_big_5_score(user_id=1, age=23, sex="Male")
    mock_evaluate.assert_called_once()
    assert result == {"O": 60}


# ------------------------------
# 5️⃣ Personalized Advice 생성
# ------------------------------
def test_extract_personalized_advice(service, mocker, mock_analysis_repo):
    mock_llm = mocker.Mock()
    mock_llm.invoke.return_value = "Be mindful of your goals."
    mocker.patch("app.features.analysis.service.ChatOpenAI", return_value=mock_llm)
    mocker.patch("app.features.analysis.service.random.choice", return_value="CBT")

    response = service.extract_personalized_advice(user_id=1, age=23, sex="Male")
    assert "Be mindful" in response
