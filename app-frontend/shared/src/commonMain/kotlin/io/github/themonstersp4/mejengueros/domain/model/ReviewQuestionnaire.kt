package io.github.themonstersp4.mejengueros.domain.model

data class ReviewQuestionnaireOption(
    val answerKey: String,
    val label: String,
)

data class ReviewQuestionnaireQuestion(
    val questionKey: String,
    val label: String,
    val options: List<ReviewQuestionnaireOption>,
)

data class ReviewQuestionnaireAnswer(
    val questionKey: String,
    val answerKey: String,
)

val reviewQuestionnaireQuestions =
    listOf(
        ReviewQuestionnaireQuestion(
            questionKey = "FIELD_CONDITION",
            label = "Estado de la cancha",
            options =
                listOf(
                    ReviewQuestionnaireOption("GOOD", "Buena"),
                    ReviewQuestionnaireOption("REGULAR", "Regular"),
                    ReviewQuestionnaireOption("BAD", "Mala"),
                ),
        ),
        ReviewQuestionnaireQuestion(
            questionKey = "LIGHTING",
            label = "Iluminación",
            options =
                listOf(
                    ReviewQuestionnaireOption("GOOD", "Buena"),
                    ReviewQuestionnaireOption("REGULAR", "Regular"),
                    ReviewQuestionnaireOption("BAD", "Mala"),
                ),
        ),
        ReviewQuestionnaireQuestion(
            questionKey = "WOULD_RETURN",
            label = "¿Volverías a jugar ahí?",
            options =
                listOf(
                    ReviewQuestionnaireOption("YES", "Sí"),
                    ReviewQuestionnaireOption("MAYBE", "Tal vez"),
                    ReviewQuestionnaireOption("NO", "No"),
                ),
        ),
    )

fun Map<String, String>.toReviewQuestionnaireAnswers(): List<ReviewQuestionnaireAnswer> =
    reviewQuestionnaireQuestions.mapNotNull { question ->
      this[question.questionKey]?.let { answerKey ->
        ReviewQuestionnaireAnswer(questionKey = question.questionKey, answerKey = answerKey)
      }
    }

fun Map<String, String>.hasRequiredReviewQuestionnaireAnswers(): Boolean =
    reviewQuestionnaireQuestions.all { question -> this[question.questionKey] != null }
