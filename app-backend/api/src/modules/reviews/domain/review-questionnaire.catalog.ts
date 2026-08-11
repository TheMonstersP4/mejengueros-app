export interface IReviewQuestionnaireOption {
  answerKey: string;
  label: string;
}

export interface IReviewQuestionnaireQuestion {
  questionKey: string;
  label: string;
  options: readonly IReviewQuestionnaireOption[];
}

export interface IReviewQuestionnaireAnswer {
  questionKey: string;
  answerKey: string;
}

export const REVIEW_QUESTIONNAIRE: readonly IReviewQuestionnaireQuestion[] = [
  {
    questionKey: 'FIELD_CONDITION',
    label: 'Estado de la cancha',
    options: [
      { answerKey: 'GOOD', label: 'Buena' },
      { answerKey: 'REGULAR', label: 'Regular' },
      { answerKey: 'BAD', label: 'Mala' }
    ]
  },
  {
    questionKey: 'LIGHTING',
    label: 'Iluminacion',
    options: [
      { answerKey: 'GOOD', label: 'Buena' },
      { answerKey: 'REGULAR', label: 'Regular' },
      { answerKey: 'BAD', label: 'Mala' }
    ]
  },
  {
    questionKey: 'WOULD_RETURN',
    label: 'Volverias a jugar ahi',
    options: [
      { answerKey: 'YES', label: 'Si' },
      { answerKey: 'MAYBE', label: 'Tal vez' },
      { answerKey: 'NO', label: 'No' }
    ]
  }
] as const;

export const REVIEW_QUESTIONNAIRE_REQUIRED_QUESTION_KEYS = REVIEW_QUESTIONNAIRE.map(
  (question) => question.questionKey
);

export const REVIEW_QUESTIONNAIRE_REQUIRED_COUNT =
  REVIEW_QUESTIONNAIRE_REQUIRED_QUESTION_KEYS.length;
