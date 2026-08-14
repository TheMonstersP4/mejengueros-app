CREATE TABLE "mejengueros_dev"."ReviewQuestionnaireAnswer" (
  "id" TEXT NOT NULL,
  "reviewId" TEXT NOT NULL,
  "questionKey" TEXT NOT NULL,
  "answerKey" TEXT NOT NULL,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT "ReviewQuestionnaireAnswer_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "ReviewQuestionnaireAnswer_reviewId_questionKey_key"
  ON "mejengueros_dev"."ReviewQuestionnaireAnswer"("reviewId", "questionKey");

CREATE INDEX "ReviewQuestionnaireAnswer_questionKey_answerKey_idx"
  ON "mejengueros_dev"."ReviewQuestionnaireAnswer"("questionKey", "answerKey");

ALTER TABLE "mejengueros_dev"."ReviewQuestionnaireAnswer"
  ADD CONSTRAINT "ReviewQuestionnaireAnswer_reviewId_fkey"
  FOREIGN KEY ("reviewId")
  REFERENCES "mejengueros_dev"."Review"("id")
  ON DELETE CASCADE
  ON UPDATE CASCADE;
