from app.features.selfaware.service import (
    QuestionService,
    ValueScoreService
)

def test_generate_single_category_question(mocker):
    question_repo = mocker.stub("QuestionRepository")
    question_repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    journal_repo = mocker.stub("JournalRepository")
    question_service = QuestionService(journal_repository=journal_repo,
                                       question_repository=question_repo)
    result = question_service.generate_single_category_question(1)
    
    assert type(result) == str
    question_repo.create_question.assert_called_once()

def test_generate_multi_category_question(mocker):
    question_repo = mocker.stub("QuestionRepository")
    question_repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    journal_repo = mocker.stub("JournalRepository")
    question_service = QuestionService(journal_repository=journal_repo,
                                       question_repository=question_repo)
    result = question_service.generate_multi_category_question(1)
    
    assert type(result) == str
    question_repo.create_question.assert_called_once()


def test_generate_selfaware_question(mocker):
    class FakeJournal:
        def __init__(self, content):
            self.content: str = content

    question_repo = mocker.stub("QuestionRepository")
    question_repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    journal_repo = mocker.stub("JournalRepository")
    journal_repo.list_journals_by_user = mocker.MagicMock(return_value = [FakeJournal("오늘은 치킨을 먹었다.")])
    question_service = QuestionService(journal_repository=journal_repo,
                                       question_repository=question_repo)
    result = question_service.generate_single_category_question(1)
    
    assert type(result) == str
    question_repo.create_question.assert_called_once()

def test_extract_value_score_from_answer(mocker):
    class FakeAnswer:
        def __init__(self, text):
            self.text: str = text
    class FakeQuestion:
        def __init__(self, text):
            self.text: str = text
    question_repo = mocker.stub("QuestionRepository")
    question_repo.get_question_by_id = mocker.MagicMock(return_value = FakeQuestion("치킨을 먹으며 느낀 행복은 무엇 때문이었나요?"))
    answer_repo = mocker.stub("AnswerRepository")
    answer_repo.get_answer_by_id = mocker.MagicMock(return_value = FakeAnswer("내 돈으로 벌어 내가 산 치킨을 먹으며 나 자신이 증명됨을 느꼈기 때문입니다."))
    value_score_repo = mocker.stub("ValueScoreRepository")
    value_score_repo.create_value_score = mocker.MagicMock(return_value = True)
    value_map_repo = mocker.stub("ValueMapRepository")
    value_map_repo.get_by_user = mocker.MagicMock(return_value = True)
    value_map_repo.update_by_value_score = mocker.MagicMock(return_value = True)
    value_score_service = ValueScoreService(question_repository=question_repo,
                                            answer_repository=answer_repo,
                                            value_score_repository=value_score_repo,
                                            value_map_repository=value_map_repo)
    
    result = value_score_service.extract_value_score_from_answer(1,1,1)

    for result_element in result:
        assert result_element.category_key in ["Neuroticism", "Extraversion", "Openness to Experience", "Agreeableness", "Conscientiousness"]
        assert type(result_element.value) == str
        assert 0 <= result_element.confidence <= 1
        assert 0 <= result_element.intensity <= 1
        assert result_element.polarity in [-1, 0, 1]
        assert type(result_element.evidence) == str

def test_get_top_value_scores(mocker):
    return

def test_generate_comment(mocker):
    return