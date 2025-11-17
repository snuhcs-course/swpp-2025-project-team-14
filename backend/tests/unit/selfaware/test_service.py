from app.features.selfaware.service import (
    QuestionService
)

def test_generate_single_category_question_fixed(mocker):
    question_repo = mocker.stub("QuestionRepository")
    question_repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    journal_repo = mocker.stub("JournalRepository")
    question_service = QuestionService(journal_repository=journal_repo,
                                       question_repository=question_repo)
    result = question_service.generate_single_category_question(1)
    
    assert type(result) == str
    question_repo.create_question.assert_called_once()