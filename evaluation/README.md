# Evaluation

`cases.json` contains 30 evaluation cases: 10 single-document answerable questions, 10 multi-document answerable questions, and 10 questions that must be refused. It also flags handwritten cases. The public corpus manifest is in [corpus/README.md](corpus/README.md); it specifies a 60-page/slide, five-format, self-authored or public-domain demo corpus without committing private notes.

Upload that corpus to a running CourseLens instance, then run:

`node evaluation/run-evaluation.mjs evaluation/cases.json http://localhost:8080`

The harness checks answer/refusal status, citation document/page coverage, multi-document cases, and source presence. It prints measured results only; no scores are committed or implied by this repository.
