# Evaluation

Put questions in `cases.json` using `cases.example.json` as a template. After running CourseLens, execute:

`node evaluation/run-evaluation.mjs evaluation/cases.json http://localhost:8080`

It reports status accuracy, refusal accuracy, source-document/page coverage, and an overall score. It does not manufacture benchmark scores.
