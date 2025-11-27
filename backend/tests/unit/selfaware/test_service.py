import pytest
from app.features.selfaware.service import (
    QuestionService,
    ValueScoreService,
    ValueMapService,
    AnswerService
)

@pytest.fixture
def mock_question_repo(mocker):
    repo = mocker.Mock()
    repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    repo.get_question_by_id.return_value = mocker.Mock(text="치킨을 먹으며 느낀 행복은 무엇 때문이었나요?")
    return repo

@pytest.fixture
def mock_journal_repo(mocker):
    repo = mocker.Mock()
    repo.list_journals_by_user.return_value = [mocker.Mock(content="오늘은 치킨을 먹었다.")]
    return repo

@pytest.fixture
def mock_answer_repo(mocker):
    repo = mocker.Mock()
    repo.get_answer_by_id.return_value = mocker.Mock(text="내 돈으로 벌어 내가 산 치킨을 먹으며 나 자신이 증명됨을 느꼈기 때문입니다.")
    repo.get_answer_by_question.return_value = mocker.Mock(text="내 돈으로 벌어 내가 산 치킨을 먹으며 나 자신이 증명됨을 느꼈기 때문입니다.")
    return repo

@pytest.fixture
def mock_value_score_repo(mocker):
    repo = mocker.Mock()
    repo.get_top_5_value_scores.return_value = [mocker.Mock(polarity=1,value="친절",intensity=90,category="Neuroticism"),
                                                mocker.Mock(polarity=1,value="사교성",intensity=90,category="Extraversion"),
                                                mocker.Mock(polarity=-1,value="이기성",intensity=85,category="Openness to Experience"),
                                                mocker.Mock(polarity=1,value="진취성",intensity=80,category="Agreeableness"),
                                                mocker.Mock(polarity=0,value="공상",intensity=75,category="성실성")]
    return repo

@pytest.fixture
def mock_value_map_repo(mocker):
    repo = mocker.Mock()
    repo.update_by_value_score.return_value = True
    repo.get_by_user.return_value = mocker.Mock(score_0=11.1, score_1=22.2, score_2=33.3, score_3=44.4, score_4=55.5)
    repo.generate_comment = mocker.MagicMock(side_effect=lambda user_id, personality_insight, comment: (personality_insight, comment))
    return repo

@pytest.fixture
def question_service(mock_journal_repo, mock_question_repo):
    return QuestionService(journal_repository=mock_journal_repo, question_repository=mock_question_repo)

@pytest.fixture
def value_score_service(mock_question_repo, mock_answer_repo, mock_value_score_repo, mock_value_map_repo):
    return ValueScoreService(question_repository=mock_question_repo,
                             answer_repository=mock_answer_repo,
                             value_score_repository=mock_value_score_repo,
                             value_map_repository=mock_value_map_repo)

@pytest.fixture
def value_map_service(mock_value_map_repo, mock_value_score_repo, mock_answer_repo):
    return ValueMapService(value_map_repository=mock_value_map_repo,
                           value_score_repository=mock_value_score_repo,
                           answer_repository=mock_answer_repo)

@pytest.fixture
def answer_service(mock_answer_repo):
    return AnswerService(answer_repository=mock_answer_repo)

def test_generate_question_type_0(question_service, mock_question_repo, mocker):
    mocker.patch("app.features.selfaware.service.random.randint", return_value = 0)
    result = question_service.generate_question(1)
    assert type(result) == str
    mock_question_repo.create_question.assert_called_once()

def test_generate_question_type_1(question_service, mock_question_repo, mocker):
    mocker.patch("app.features.selfaware.service.random.randint", return_value = 1)
    result = question_service.generate_question(1)
    assert type(result) == str
    mock_question_repo.create_question.assert_called_once()

def test_generate_question_type_2(question_service, mock_question_repo, mocker):
    mocker.patch("app.features.selfaware.service.random.randint", return_value = 2)
    result = question_service.generate_question(1)
    assert type(result) == str
    mock_question_repo.create_question.assert_called_once()

def test_extract_value_score_from_answer(value_score_service):    
    result = value_score_service.extract_value_score_from_answer(1,1,1)
    for result_element in result:
        assert result_element.category_key in ["Neuroticism", "Extraversion", "Openness to Experience", "Agreeableness", "Conscientiousness"]
        assert type(result_element.value) == str
        assert 0 <= result_element.confidence <= 1
        assert 0 <= result_element.intensity <= 1
        assert result_element.polarity in [-1, 0, 1]
        assert type(result_element.evidence) == str

def test_get_top_value_scores(value_score_service):    
    result = value_score_service.get_top_value_scores(1)
    for result_element in result:
        assert type(result_element["value"]) == str
        assert type(result_element["intensity"]) == int
    assert len(result) == 4

def test_generate_comment(value_map_service):
    result = value_map_service.generate_comment(1)
    assert type(result[0]) == str
    assert type(result[1]) == str

def test_get_answer_by_question(answer_service):
    result = answer_service.get_answer_by_question(1)
    assert result