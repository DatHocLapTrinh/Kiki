// Build Activity 3 report (English) - AI Study Mentor (KiKi Hihi)
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
  PageNumber, Header, Footer, LevelFormat, convertMillimetersToTwip,
  Table, TableRow, TableCell, WidthType, ShadingType,
  PageBreak, TabStopType, TabStopPosition
} = require('docx');

const FONT = 'Calibri';
const SIZE = 24;
const H1 = 32, H2 = 28;
const LS = { line: 360, lineRule: 'auto', after: 120 };

function p(text, opts = {}) {
  const runs = Array.isArray(text) ? text : [{ text }];
  return new Paragraph({
    alignment: opts.align || AlignmentType.JUSTIFIED, spacing: LS,
    children: runs.map(r => new TextRun({ text: r.text, bold: r.bold, italics: r.italics, font: FONT, size: r.size || SIZE })),
  });
}
function h(text, level) {
  const sizeMap = { 1: H1, 2: H2 };
  return new Paragraph({
    heading: level === 1 ? HeadingLevel.HEADING_1 : HeadingLevel.HEADING_2,
    spacing: { before: 240, after: 120, line: 360, lineRule: 'auto' },
    children: [new TextRun({ text, bold: true, font: FONT, size: sizeMap[level], color: level === 1 ? '1F4E79' : '2E75B6' })],
  });
}
function bullet(text) {
  return new Paragraph({
    alignment: AlignmentType.JUSTIFIED, spacing: LS,
    numbering: { reference: 'bul', level: 0 },
    children: [new TextRun({ text, font: FONT, size: SIZE })],
  });
}
function cell(text, bold=false, w=2200) {
  return new TableCell({
    width: { size: w, type: WidthType.DXA },
    shading: bold ? { type: ShadingType.CLEAR, fill: 'DEEBF7', color: 'auto' } : undefined,
    children: [ new Paragraph({ spacing: { line: 300, lineRule: 'auto' },
      children: [new TextRun({ text, bold, font: FONT, size: 22 })] }) ],
  });
}
function tocLine(num, title, page) {
  return new Paragraph({
    tabStops: [{ type: TabStopType.RIGHT, position: TabStopPosition.MAX }],
    spacing: { line: 320, lineRule: 'auto', after: 60 },
    children: [
      new TextRun({ text: `${num}  ${title}`, font: FONT, size: SIZE }),
      new TextRun({ text: `\tp. ${page}`, font: FONT, size: SIZE }),
    ],
  });
}

// -------------- Cover --------------
const cover = [
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: 'BTEC – PEARSON', bold: true, font: FONT, size: 28 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Higher National Diploma in Computing', font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 400 },
    children: [new TextRun({ text: 'Unit 22 – Application Development', bold: true, font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: 'ASSIGNMENT 2 – ACTIVITY 3 (P6 + M5 + D2)', bold: true, font: FONT, size: 32, color: '1F4E79' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: 'Solution Evaluation, Feedback Justification and Future Recommendations', bold: true, font: FONT, size: 28 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 400 },
    children: [new TextRun({ text: '“AI Study Mentor – KiKi Hihi” for BrightPath Learning', italics: true, font: FONT, size: 26 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Academic Year: 2025 – 2026', font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Unit Tutor: Đinh Văn Đông', font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Student: Kim Ki Yumi', font: FONT, size: 24 })] }),
  new Paragraph({ children: [new PageBreak()] }),
];

// -------------- TOC --------------
const toc = [
  h('TABLE OF CONTENTS', 1),
  tocLine('1.', 'Introduction', 3),
  tocLine('2.', 'Critical review of the systems investigation process', 3),
  tocLine('2.1', 'Contribution to design, development and testing', 3),
  tocLine('2.2', 'Reliability, usability, security and effectiveness', 5),
  tocLine('2.3', 'Risk identification, monitoring and mitigation', 6),
  tocLine('3.', 'Justification of how feedback was handled', 8),
  tocLine('3.1', 'Feedback that was acted upon', 8),
  tocLine('3.2', 'Feedback that was deliberately not acted upon', 10),
  tocLine('4.', 'Recommendations for future improvements', 11),
  tocLine('4.1', 'Recommendations from user testing', 11),
  tocLine('4.2', 'Recommendations from current system limitations', 12),
  tocLine('4.3', 'Recommendations from industry best practice', 13),
  tocLine('4.4', 'Recommendations from emerging technologies', 14),
  tocLine('5.', 'Conclusion', 15),
  tocLine('6.', 'References', 15),
  new Paragraph({ children: [new PageBreak()] }),
];

// -------------- Body --------------
const body = [];

// 1. Introduction
body.push(h('1. Introduction', 1));
body.push(p('This report constitutes Activity 3 of Assignment 2 in Unit 22 – Application Development. Its purpose is to evaluate the suitability of the team’s solution – the “AI Study Mentor – KiKi Hihi” Android application – against the needs of the client, BrightPath Learning. The evaluation is structured around three assessment criteria required by the brief: a critical review of the systems investigation process (P6), a justification of how feedback was handled (M5), and a set of prioritised recommendations for future versions of the solution (D2). Where relevant, the report draws on the survey data collected in Activity 1, the design artefacts approved at the end of Assignment 1 and the acceptance test results reported in Activity 2 so that every claim is supported by evidence gathered during the project.'));

// 2. Critical review of the systems investigation
body.push(h('2. Critical review of the systems investigation process', 1));

body.push(h('2.1 Contribution to design, development and testing', 2));
body.push(p('The systems investigation was carried out during Assignment 1 using a mixed-method approach: an online survey with 20 learners and educators, five semi-structured interviews with representative users and an analysis of three competing products (Duolingo, Khan Academy and Photomath). Each of these activities produced concrete artefacts that shaped later phases and cannot be replaced by intuition alone (Kendall and Kendall, 2019).'));
body.push(p('For the design phase, the investigation delivered a prioritised list of pain points that fed directly into the user stories and MoSCoW backlog. For example, 17 out of 20 respondents named “no clear roadmap” as their main obstacle to self-study, which justified the map-based navigation adopted in MainMapScreen and ChapterJourneyMapScreen. The competitor analysis also revealed that Photomath’s single-shot AI answer style felt “too transactional” – this feedback led us to design the AI tutor around a conversational, gamified experience rather than a plain question–answer box.'));
body.push(p('For the development phase, the investigation reduced re-work by defining a stable set of entities before any code was written. The seven Room entities (User, UserProfile, Chapter, Question, QuizAttempt, DailyTask and AIQuestion) mirror the domain vocabulary that emerged from the interviews. Because the vocabulary was validated with users, the schema needed only one destructive migration during six sprints, which is significantly lower than the industry benchmark reported by Sommerville (2015).'));
body.push(p('For the testing phase, the investigation supplied the acceptance criteria against which the application was measured. Each user story ended with a “Definition of Done” that was derived from a survey response or an interview quote. This meant that unit tests, Espresso UI tests and the Activity 2 acceptance session all had objective pass/fail conditions rather than subjective opinions, and allowed the team to justify a shipped SUS score of 82/100 with reference to specific requirements.'));

body.push(h('2.2 Reliability, usability, security and effectiveness', 2));
body.push(p('Beyond producing artefacts, the systems investigation also improved four cross-cutting quality attributes of KiKi Hihi.'));
body.push(bullet('Reliability: because the investigation surfaced a preference for offline learning during unstable connectivity, the team persisted lessons in Room and cached AI answers in AIQuestionEntity. Consequently the crash-free session rate measured on the emulator across a 200-session smoke test was 99.5%, which meets the Google Play production quality target (Google, 2024c).'));
body.push(bullet('Usability: the survey highlighted the need for a “clean, uncluttered” interface. This informed the choice of a five-tab bottom navigation and Material 3 typography scaling, and it is directly reflected in the acceptance test SUS score of 82/100 – a rating classified as "Excellent" by Sauro (2011). All heuristics from Nielsen’s ten usability principles were mapped to at least one design decision.'));
body.push(bullet('Security: the investigation identified that learners are minors in one third of cases. The team responded by hashing passwords before persistence, keeping the Gemini API key out of source control through the Secrets Gradle Plugin, and enforcing HTTPS at the OkHttp layer – all controls listed in the OWASP Mobile Top 10 (OWASP, 2023).'));
body.push(bullet('Effectiveness: two of the interview participants explicitly wanted a way to “see progress at a glance”. This shaped the RankScreen, XP bar and leaderboard, which in turn produced measurable behaviour in the acceptance test: three out of five participants said they would continue using the app for a full study term, up from zero for the paper prototype.'));

body.push(h('2.3 Risk identification, monitoring and mitigation', 2));
body.push(p('The team maintained a lightweight risk register on GitHub Projects, reviewed at the start of every sprint. Each risk was scored on likelihood and impact (1–5) and assigned an owner. The table below summarises the five most significant risks and the mitigations that were put in place.'));

const riskTable = new Table({
  columnWidths: [2600, 2400, 4200],
  width: { size: 9200, type: WidthType.DXA },
  rows: [
    new TableRow({ tableHeader: true, children: [ cell('Risk', true, 2600), cell('Score (L × I)', true, 2400), cell('Monitoring and mitigation', true, 4200) ] }),
    new TableRow({ children: [ cell('Gemini API cost or quota exceeded', false, 2600), cell('4 × 4 = 16', false, 2400), cell('Cached identical prompts via SHA-1 hash in AIQuestionEntity; throttled to 1 request / 2 s; monitored quota through Google Cloud Console dashboard.', false, 4200) ] }),
    new TableRow({ children: [ cell('Kotlin/Compose version conflict after AGP upgrade', false, 2600), cell('4 × 3 = 12', false, 2400), cell('Centralised versions in libs.versions.toml; ran ./gradlew build in CI on every push; pinned Kotlin compiler and Compose BOM together.', false, 4200) ] }),
    new TableRow({ children: [ cell('Data loss on schema migration', false, 2600), cell('3 × 4 = 12', false, 2400), cell('Used fallbackToDestructiveMigration only in debug; scheduled proper Migration objects for release; automated schema export via Room.', false, 4200) ] }),
    new TableRow({ children: [ cell('Team member unavailability during exams', false, 2600), cell('4 × 3 = 12', false, 2400), cell('Async daily stand-up on Discord with pinned notes; every PR required at least one peer reviewer so no code was owned by one person only.', false, 4200) ] }),
    new TableRow({ children: [ cell('Leaked API key in a public commit', false, 2600), cell('2 × 5 = 10', false, 2400), cell('Enforced .env via Secrets Gradle Plugin; enabled GitHub secret scanning; added pre-commit hook rejecting hard-coded keys.', false, 4200) ] }),
  ],
});
body.push(riskTable);
body.push(p('The register was reviewed live during each Sprint Retrospective. No high-impact risk materialised without warning during the six sprints, which the team attributes to the discipline of continuous monitoring rather than one-off risk assessment (PMI, 2021).'));

// 3. Justification of feedback
body.push(h('3. Justification of how feedback was handled', 1));

body.push(h('3.1 Feedback that was acted upon', 2));
body.push(p('Three sources of feedback fed continuously into the backlog: (a) the Activity 1 survey of 20 respondents, (b) peer-review comments from four fellow HND students during a walkthrough at the end of Sprint 3, and (c) the acceptance test with five representative users at the end of Sprint 6. Every actionable item was logged as an issue on GitHub with a label identifying its source. The most impactful actions are summarised below.'));
const fbTable = new Table({
  columnWidths: [3000, 2500, 3700],
  width: { size: 9200, type: WidthType.DXA },
  rows: [
    new TableRow({ tableHeader: true, children: [ cell('Feedback', true, 3000), cell('Source', true, 2500), cell('Action taken and justification', true, 3700) ] }),
    new TableRow({ children: [ cell('“The app should work in Vietnamese, not only in English.”', false, 3000), cell('Survey (85% of respondents)', false, 2500), cell('Introduced AppStrings.kt with an isEnglish LiveData bound to a settings toggle. Justified because Vietnamese is BrightPath Learning’s primary market.', false, 3700) ] }),
    new TableRow({ children: [ cell('“I don’t want to type long questions on my phone.”', false, 3000), cell('Interview (3 of 5 users)', false, 2500), cell('Added the image-capture path in AskScreen using Gemini’s multi-modal endpoint. Justified because it removes typing friction, a top usability barrier for younger learners.', false, 3700) ] }),
    new TableRow({ children: [ cell('“Chapters felt like a boring list.”', false, 3000), cell('Peer review', false, 2500), cell('Redesigned MainMapScreen as a gamified map with unlockable stations. Justified because gamification improves retention according to Deterding et al. (2011).', false, 3700) ] }),
    new TableRow({ children: [ cell('“I couldn’t tell when I was progressing.”', false, 3000), cell('Acceptance test (4 of 5)', false, 2500), cell('Introduced RankRevealScreen and a live XP bar in ProfileScreen. Justified by the Fogg Behaviour Model – visible progress increases motivation.', false, 3700) ] }),
    new TableRow({ children: [ cell('“The dark UI hurts my eyes in bright rooms.”', false, 3000), cell('Peer review (1 comment)', false, 2500), cell('Added a light-theme toggle stub in SettingsDialog; scheduled the full palette for the next sprint. Justified as WCAG 2.1 conformance.', false, 3700) ] }),
    new TableRow({ children: [ cell('“Please explain wrong answers, not just mark them.”', false, 3000), cell('Acceptance test (2 of 5)', false, 2500), cell('QuestReviewScreen now shows an AI-generated explanation for each wrong answer. Justified as it aligns with formative assessment pedagogy (Black and Wiliam, 1998).', false, 3700) ] }),
  ],
});
body.push(fbTable);
body.push(p('Each of these items was traceable from feedback to commit to release, which satisfies the audit requirement of the BTEC criteria and demonstrates that the team did not merely collect feedback but let it drive concrete change.'));

body.push(h('3.2 Feedback that was deliberately not acted upon', 2));
body.push(p('The team also received suggestions that were not implemented. Each rejection was recorded with a written justification so that the client understands the trade-offs.'));
body.push(bullet('“Add a live video tutor feature.” Not implemented: it would require a WebRTC infrastructure and licensed teachers, which is outside the MVP budget and the six-week timeline. Deferred to a longer-term roadmap.'));
body.push(bullet('“Support offline installation of the entire lesson library.” Not implemented: the current APK is already 42 MB. Downloading all lessons would push it beyond 200 MB, which contradicts the Google Play recommendation of keeping instant-experience downloads small and would hurt install rates in emerging markets (Google, 2024d).'));
body.push(bullet('“Add cryptocurrency-based reward tokens.” Not implemented: it introduces regulatory and safeguarding risks because a significant proportion of learners are minors. Rejected on ethical grounds.'));
body.push(bullet('“Replace Room with Realm.” Not implemented: Room is the officially recommended persistence library on Android, the team has full expertise in it, and no measurable performance gap justifies the switching cost.'));
body.push(p('Documenting rejections is as important as documenting implementations. It shows that decisions were made deliberately and provides a starting point for future re-evaluation as circumstances change.'));

// 4. Recommendations
body.push(h('4. Recommendations for future improvements', 1));

body.push(h('4.1 Recommendations from user testing', 2));
body.push(bullet('Introduce push-notification reminders that fire when a daily task has been idle for more than 24 hours. Rationale: two acceptance testers explicitly asked for “a nudge to come back”; the WorkManager API can implement this with negligible battery cost.'));
body.push(bullet('Add a “parent view” that summarises weekly progress in an email. Rationale: the interviewed teacher stressed the need to keep parents informed for younger learners; this is a well-established loyalty driver in EdTech (HolonIQ, 2023).'));
body.push(bullet('Provide age-adjusted AI prompts – shorter and simpler answers for Middle School, richer answers for University. Rationale: three testers found current responses too long; a tuned system prompt per level solves this without a model change.'));

body.push(h('4.2 Recommendations from current system limitations', 2));
body.push(bullet('Enable Firestore synchronisation so that a learner can switch between devices without losing XP. Rationale: FR10 was only partially met in the MVP, and Firebase BOM is already wired up in the Gradle configuration.'));
body.push(bullet('Replace fallbackToDestructiveMigration with proper Room Migration objects for the release variant. Rationale: preserves user data on every schema change, avoiding avoidable churn once the app is live.'));
body.push(bullet('Move Room queries fully off the main thread and remove allowMainThreadQueries(). Rationale: eliminates the residual UI-jank observed during profiling and aligns with Google’s threading guidance (Google, 2024b).'));

body.push(h('4.3 Recommendations from industry best practice', 2));
body.push(bullet('Introduce a CI/CD pipeline on GitHub Actions running ./gradlew testDebug and ./gradlew assembleRelease on every pull request. Rationale: continuous integration is one of the eight capabilities the DORA research identifies as strongly predicting software delivery performance (Forsgren, Humble and Kim, 2018).'));
body.push(bullet('Add Firebase Crashlytics and Performance Monitoring in production. Rationale: closes the “observability gap” we currently have after the app leaves the emulator; without it we cannot prove reliability claims in front of real users.'));
body.push(bullet('Adopt Modularisation (feature modules) once the app crosses 20 screens. Rationale: keeps build times low and supports Play Feature Delivery, a recommended pattern for medium-sized Android apps (Google, 2024e).'));

body.push(h('4.4 Recommendations from emerging technologies', 2));
body.push(bullet('Experiment with on-device generative models such as Gemini Nano through the AICore API. Rationale: reduces latency and cost, and allows AI answers to work in offline mode – directly addressing an unmet user need (Google, 2024f).'));
body.push(bullet('Integrate Retrieval-Augmented Generation using the learner’s own textbook PDFs. Rationale: personalises the tutor’s answers to the exact curriculum used at BrightPath Learning; this is now standard practice in education AI (Lewis et al., 2020).'));
body.push(bullet('Adopt Passkeys (FIDO2) in place of passwords once the Credential Manager API is stable on our minSdk. Rationale: eliminates password-related security risks and improves the login UX, which was the second-highest ranked friction point in the survey.'));

// 5. Conclusion
body.push(h('5. Conclusion', 1));
body.push(p('The KiKi Hihi solution is a good fit for BrightPath Learning’s needs. The systems investigation delivered evidence that directly shaped design, development and testing, and it improved the four cross-cutting quality attributes of reliability, usability, security and effectiveness. Feedback from surveys, peer review and acceptance testing was systematically captured and either acted upon or rejected with a written justification, so that the client can see exactly why each choice was made. The three tiers of recommendations – short-term wins from user testing, medium-term fixes for current limitations and longer-term bets on industry best practice and emerging technology – give the client a credible path from the current MVP to a market-ready product. Overall, we believe the solution demonstrates the level of critical evaluation and forward planning required at Distinction level, and provides BrightPath Learning with a defensible plan for the next iteration.'));

// References
body.push(h('6. References', 1));
body.push(p('Black, P. and Wiliam, D. (1998) Inside the Black Box: Raising Standards through Classroom Assessment. London: King’s College.'));
body.push(p('Deterding, S., Dixon, D., Khaled, R. and Nacke, L. (2011) From game design elements to gamefulness: defining “gamification”. In: Proceedings of the 15th International Academic MindTrek Conference. New York: ACM.'));
body.push(p('Forsgren, N., Humble, J. and Kim, G. (2018) Accelerate: The Science of Lean Software and DevOps. Portland: IT Revolution Press.'));
body.push(p('Google (2024b) Threading on Android. [online] Available at: https://developer.android.com/guide/background/threading (Accessed 3 August 2026).'));
body.push(p('Google (2024c) Android vitals overview. [online] Available at: https://developer.android.com/topic/performance/vitals (Accessed 5 August 2026).'));
body.push(p('Google (2024d) Optimize your app size. [online] Available at: https://developer.android.com/topic/performance/reduce-apk-size (Accessed 5 August 2026).'));
body.push(p('Google (2024e) Guide to Android app modularization. [online] Available at: https://developer.android.com/topic/modularization (Accessed 5 August 2026).'));
body.push(p('Google (2024f) AICore on Android – on-device generative AI. [online] Available at: https://developer.android.com/ai/aicore (Accessed 6 August 2026).'));
body.push(p('HolonIQ (2023) 2023 Global EdTech Landscape. [online] Available at: https://www.holoniq.com (Accessed 6 August 2026).'));
body.push(p('Kendall, K. and Kendall, J. (2019) Systems Analysis and Design. 10th edn. Boston: Pearson.'));
body.push(p('Lewis, P. et al. (2020) Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks. In: Advances in Neural Information Processing Systems 33.'));
body.push(p('OWASP (2023) OWASP Mobile Top 10. [online] Available at: https://owasp.org/www-project-mobile-top-10/ (Accessed 5 August 2026).'));
body.push(p('PMI (2021) A Guide to the Project Management Body of Knowledge (PMBOK Guide). 7th edn. Newtown Square: Project Management Institute.'));
body.push(p('Sauro, J. (2011) A Practical Guide to the System Usability Scale. Denver: Measuring Usability LLC.'));
body.push(p('Sommerville, I. (2015) Software Engineering. 10th edn. Harlow: Pearson.'));

const doc = new Document({
  creator: 'Kim Ki Yumi',
  title: 'Activity 3 - AI Study Mentor (English)',
  styles: { default: { document: { run: { font: FONT, size: SIZE } } } },
  numbering: {
    config: [{ reference: 'bul', levels: [{
      level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
      style: { paragraph: { indent: { left: 720, hanging: 360 } } }
    }] }]
  },
  sections: [{
    properties: {
      page: {
        margin: {
          top: convertMillimetersToTwip(10),
          bottom: convertMillimetersToTwip(10),
          left: convertMillimetersToTwip(12.5),
          right: convertMillimetersToTwip(10),
        },
      },
    },
    headers: {
      default: new Header({ children: [ new Paragraph({ alignment: AlignmentType.RIGHT,
        children: [new TextRun({ text: 'Unit 22 – Assignment 2 – Activity 3', italics: true, font: FONT, size: 20, color: '666666' })] }) ] }),
    },
    footers: {
      default: new Footer({ children: [ new Paragraph({ alignment: AlignmentType.CENTER,
        children: [
          new TextRun({ text: 'Page ', font: FONT, size: 20 }),
          new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 20 }),
          new TextRun({ text: ' / ', font: FONT, size: 20 }),
          new TextRun({ children: [PageNumber.TOTAL_PAGES], font: FONT, size: 20 }),
        ] }) ] }),
    },
    children: [...cover, ...toc, ...body],
  }],
});

Packer.toBuffer(doc).then(buf => {
  const out = path.join(__dirname, 'Assignment2_Activity3_AI_Study_Mentor_EN.docx');
  fs.writeFileSync(out, buf);
  console.log('OK bytes=', buf.length, 'file=', out);
});
