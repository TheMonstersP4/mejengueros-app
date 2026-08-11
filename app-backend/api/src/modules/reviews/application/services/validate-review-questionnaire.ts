import type { IReviewQuestionnaireAnswer } from '../../domain/review-questionnaire.catalog';
import {
  REVIEW_QUESTIONNAIRE,
  REVIEW_QUESTIONNAIRE_REQUIRED_QUESTION_KEYS
} from '../../domain/review-questionnaire.catalog';
import { InvalidReviewQuestionnaireError } from '../../domain/errors/invalid-review-questionnaire.error';

export function validateReviewQuestionnaireAnswers(
  answers: readonly IReviewQuestionnaireAnswer[] | undefined
): IReviewQuestionnaireAnswer[] {
  const normalizedAnswers =
    answers?.map((answer) => ({
      questionKey: answer.questionKey.trim().toUpperCase(),
      answerKey: answer.answerKey.trim().toUpperCase()
    })) ?? [];

  const answersByQuestion = new Map<string, string>();
  const duplicateQuestionKeys: string[] = [];
  const invalidAnswers: IReviewQuestionnaireAnswer[] = [];

  for (const answer of normalizedAnswers) {
    if (answersByQuestion.has(answer.questionKey)) {
      duplicateQuestionKeys.push(answer.questionKey);
      continue;
    }

    answersByQuestion.set(answer.questionKey, answer.answerKey);
  }

  const missingQuestionKeys = REVIEW_QUESTIONNAIRE_REQUIRED_QUESTION_KEYS.filter(
    (questionKey) => !answersByQuestion.has(questionKey)
  );

  for (const question of REVIEW_QUESTIONNAIRE) {
    const answerKey = answersByQuestion.get(question.questionKey);

    if (
      answerKey != null &&
      !question.options.some((option) => option.answerKey === answerKey)
    ) {
      invalidAnswers.push({ questionKey: question.questionKey, answerKey });
    }
  }

  if (
    missingQuestionKeys.length > 0 ||
    duplicateQuestionKeys.length > 0 ||
    invalidAnswers.length > 0
  ) {
    throw new InvalidReviewQuestionnaireError({
      missingQuestionKeys,
      duplicateQuestionKeys,
      invalidAnswers
    });
  }

  return REVIEW_QUESTIONNAIRE_REQUIRED_QUESTION_KEYS.map((questionKey) => ({
    questionKey,
    answerKey: answersByQuestion.get(questionKey) as string
  }));
}
