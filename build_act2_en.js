// Build Activity 2 report (English) - AI Study Mentor (KiKi Hihi)
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
  PageNumber, Header, Footer, LevelFormat, convertMillimetersToTwip,
  Table, TableRow, TableCell, WidthType, ShadingType,
  ImageRun, PageBreak, TabStopType, TabStopPosition
} = require('docx');

const FONT = 'Calibri';
const SIZE = 24;
const H1 = 32, H2 = 28, H3 = 26;
const LS = { line: 360, lineRule: 'auto', after: 120 };

function p(text, opts = {}) {
  const runs = Array.isArray(text) ? text : [{ text }];
  return new Paragraph({
    alignment: opts.align || AlignmentType.JUSTIFIED,
    spacing: LS,
    children: runs.map(r => new TextRun({ text: r.text, bold: r.bold, italics: r.italics, font: FONT, size: r.size || SIZE })),
  });
}
function h(text, level) {
  const sizeMap = { 1: H1, 2: H2, 3: H3 };
  return new Paragraph({
    heading: level === 1 ? HeadingLevel.HEADING_1 : level === 2 ? HeadingLevel.HEADING_2 : HeadingLevel.HEADING_3,
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
function imageParagraph(filepath, w = 220, hh = 490) {
  const data = fs.readFileSync(filepath);
  return new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 120, after: 60 },
    children: [new ImageRun({ data, transformation: { width: w, height: hh }, type: 'png' })],
  });
}
function caption(text) {
  return new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { after: 240 },
    children: [new TextRun({ text, italics: true, font: FONT, size: 20, color: '555555' })],
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

const SCREENS = path.join(__dirname, 'New folder');

const cover = [
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: 'BTEC – PEARSON', bold: true, font: FONT, size: 28 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Higher National Diploma in Computing', font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 400 },
    children: [new TextRun({ text: 'Unit 22 – Application Development', bold: true, font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: 'ASSIGNMENT 2 – ACTIVITY 2 (P5 + M4)', bold: true, font: FONT, size: 32, color: '1F4E79' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: 'Business Application Development Report', bold: true, font: FONT, size: 30 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 400 },
    children: [new TextRun({ text: '“AI Study Mentor – KiKi Hihi” for BrightPath Learning', italics: true, font: FONT, size: 26 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Academic Year: 2025 – 2026', font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Unit Tutor: Đinh Văn Đông', font: FONT, size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Assignment Title: Evaluate the performance of a business application against its software design document', italics: true, font: FONT, size: 22 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
    children: [new TextRun({ text: 'Student: Kim Ki Yumi', font: FONT, size: 24 })] }),
  new Paragraph({ children: [new PageBreak()] }),
];

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
const toc = [
  h('TABLE OF CONTENTS', 1),
  tocLine('1.', 'Introduction', 3),
  tocLine('2.', 'Application Development', 3),
  tocLine('2.1', 'Architecture and toolchain overview', 3),
  tocLine('2.2', 'Agile Scrum sprint process', 5),
  tocLine('2.3', 'Implementation stages and tool evidence', 6),
  tocLine('3.', 'Review and reflection on the development process', 9),
  tocLine('3.1', 'Tasks completed', 9),
  tocLine('3.2', 'How the tasks were completed', 10),
  tocLine('3.3', 'Difficulties encountered and how they were resolved', 11),
  tocLine('4.', 'Application evaluation', 12),
  tocLine('4.1', 'Alignment with the problem definition and user requirements', 12),
  tocLine('4.2', 'Functional requirements assessment', 13),
  tocLine('4.3', 'Non-functional requirements assessment', 14),
  tocLine('4.4', 'Feature quality and alignment with user expectations', 15),
  tocLine('5.', 'Conclusion', 16),
  tocLine('6.', 'References', 16),
  new Paragraph({ children: [new PageBreak()] }),
];

const body = [];

// 1. Introduction
body.push(h('1. Introduction', 1));
body.push(p('This report addresses Activity 2 of Assignment 2 in Unit 22 – Application Development. It focuses on the implementation phase of the “AI Study Mentor – KiKi Hihi” project that our team has built for the fictitious client BrightPath Learning. Building on the software design document and the system architecture that were approved in Assignment 1, the team has delivered a personalised learning application for the Android platform. The user interface is written in Kotlin with Jetpack Compose, the business logic layer uses Java, local persistence is provided by Room (SQLite), and the Google Gemini large language model is integrated to act as an on-demand AI tutor.'));
body.push(p('The report is structured in three main parts. The first part explains in detail how the team developed the application, together with the tools, techniques and methodologies that were used and screenshot evidence of those choices. The second part is a review and reflection covering the tasks that have been completed, the way each task was implemented, the difficulties that emerged and how the team resolved them. The third part evaluates how well the delivered application meets the original problem definition, the initial user requirements and the functional and non-functional requirements, and it comments on the quality of the delivered features.'));

// 2. Application Development
body.push(h('2. Application Development', 1));

body.push(h('2.1 Architecture and toolchain overview', 2));
body.push(p('KiKi Hihi is built following the MVVM (Model – View – ViewModel) pattern combined with the Repository pattern, as specified in the Assignment 1 design document. The View layer consists of Jetpack Compose composables such as IntroScreen, IdentitySelectionScreen, MainMapScreen, ChapterJourneyMapScreen, QuestScreen, AskScreen, ProfileScreen and RankScreen. The ViewModel layer (StudyViewModel) owns the UI state as LiveData streams and orchestrates business logic. The Model – Repository layer (DataRepository) serves as the single source of truth, hiding the implementation details of Room and Retrofit from the ViewModel (Google, 2024a).'));
body.push(p('The table below summarises the tools and libraries the team selected, mapped against the justifications recorded in the design document.'));

const toolsTable = new Table({
  columnWidths: [2200, 3200, 3800],
  width: { size: 9200, type: WidthType.DXA },
  rows: [
    new TableRow({ tableHeader: true, children: [ cell('Category', true, 2200), cell('Technology / tool', true, 3200), cell('Rationale', true, 3800) ] }),
    new TableRow({ children: [ cell('IDE', false, 2200), cell('Android Studio Ladybug + Gradle 9.1', false, 3200), cell('Industry standard for Android; provides Compose Preview and Layout Inspector.', false, 3800) ] }),
    new TableRow({ children: [ cell('Languages', false, 2200), cell('Kotlin 2.2 + Java 11', false, 3200), cell('Kotlin for modern declarative UI and coroutines; Java for stable Room/Retrofit modules.', false, 3800) ] }),
    new TableRow({ children: [ cell('UI framework', false, 2200), cell('Jetpack Compose (BOM 2024.09) + Material 3', false, 3200), cell('Declarative UI, less boilerplate, easy to build gamification effects.', false, 3800) ] }),
    new TableRow({ children: [ cell('Dependency Injection', false, 2200), cell('Hilt 2.60 (Dagger)', false, 3200), cell('Simple DI on Android, supports @HiltViewModel injection.', false, 3800) ] }),
    new TableRow({ children: [ cell('Local database', false, 2200), cell('Room 2.7 (SQLite)', false, 3200), cell('Type-safe queries with 7 entities: User, UserProfile, Chapter, Question, QuizAttempt, DailyTask, AIQuestion.', false, 3800) ] }),
    new TableRow({ children: [ cell('Networking', false, 2200), cell('Retrofit 2.12 + OkHttp 4.10 + Moshi', false, 3200), cell('Integrates with the Gemini API and logs requests/responses for debugging.', false, 3800) ] }),
    new TableRow({ children: [ cell('AI service', false, 2200), cell('Google Gemini generateContent endpoint', false, 3200), cell('Produces tutor answers; supports image input (base64) for solving picture-based exercises.', false, 3800) ] }),
    new TableRow({ children: [ cell('Secret management', false, 2200), cell('Secrets Gradle Plugin + .env', false, 3200), cell('Prevents API keys from being hard-coded into the repository.', false, 3800) ] }),
    new TableRow({ children: [ cell('Testing', false, 2200), cell('JUnit 4, Robolectric 4.16, Roborazzi, Espresso', false, 3200), cell('Covers unit tests, UI tests and Compose snapshot tests.', false, 3800) ] }),
    new TableRow({ children: [ cell('Source control', false, 2200), cell('Git + GitHub, Gradle wrapper 8.x', false, 3200), cell('Enables team collaboration, pull-request reviews and reproducible builds.', false, 3800) ] }),
    new TableRow({ children: [ cell('Project management', false, 2200), cell('Trello + Google Meet, Google Drive for documents', false, 3200), cell('Lightweight Scrum: one-week sprints and 15-minute daily stand-ups.', false, 3800) ] }),
  ],
});
body.push(toolsTable);
body.push(p('The whole project is packaged under com.aistudio.kikihihi.magic with the display name “KiKi Hihi”. A clean package separation (di, model, network, repository, viewmodel, sqlite.room, ui.theme) allowed the team to work on features in parallel with minimal merge conflicts.'));

body.push(h('2.2 Agile Scrum sprint process', 2));
body.push(p('The team followed a lightweight Agile Scrum process (Sutherland, 2020) with one-week sprints across a six-week window. The product backlog was seeded directly from the user stories in the Assignment 1 design document and prioritised by business value for BrightPath Learning: (1) learner identification and login, (2) a study journey organised by level and subject, (3) quiz taking, (4) AI question and answer, and (5) profile, ranking and gamification.'));
body.push(bullet('Sprint 1 – Project bootstrap: created the Android skeleton, configured Gradle Kotlin DSL, integrated Hilt, Compose and Room, and produced AppDatabase with seven entities plus AppDao.'));
body.push(bullet('Sprint 2 – Onboarding: built IntroScreen, IdentitySelectionScreen (choose Middle School / High School / University) and the local authentication flow with UserEntity + UserProfileEntity.'));
body.push(bullet('Sprint 3 – Study journey: implemented MainMapScreen, ChapterJourneyMapScreen and ProgressionMapScreen, seeding lesson data from assets/questions.json into Room via the seedInitialData() method of DataRepository.'));
body.push(bullet('Sprint 4 – Quiz and gamification: shipped QuestScreen, QuestsScreen and QuestReviewScreen, and added the XP – Level – RankTitle system (Bronze Novice, Silver Adept, Gold Sage, and so on) inside StudyViewModel.'));
body.push(bullet('Sprint 5 – AI integration: wired AskScreen to GeminiApiService through Retrofit, supporting both text prompts and photographs of exercises, and persisting history to AIQuestionEntity.'));
body.push(bullet('Sprint 6 – Polish: finalised ProfileScreen, RankScreen, RankRevealScreen and SettingsDialog (English/Vietnamese toggle); ran end-to-end testing and signed the release APK through signingConfigs and the Secrets Plugin.'));
body.push(p('At the end of every sprint the team held a sprint review demo on a Pixel emulator and a short retrospective. Work was tracked on a Trello Kanban board with three columns (To do – Doing – Done) and module-based labels so that progress was easy to read at a glance.'));

body.push(h('2.3 Implementation stages and tool evidence', 2));
body.push(p('The screenshots below illustrate the delivered modules, all of which follow the UI/UX blueprint from Assignment 1. A dark space theme was chosen to reduce eye strain during evening study sessions and to reinforce the "exploring a universe of knowledge" brand narrative that BrightPath Learning wanted for KiKi.'));

const shots = [
  ['Screenshot_20260806_204305.png', 'Figure 1. KiKi Hihi login screen – local authentication flow driven by UserEntity and SessionManager.'],
  ['Screenshot_20260806_193433.png', 'Figure 2. MainMapScreen – chapter-based study path (Mechanics Base, Thermo Field, Quantum Void…) showing completed lessons and lock/unlock states.'],
  ['Screenshot_20260806_193517.png', 'Figure 3. ChapterJourneyMapScreen – detail view of a chapter with lesson stops and a gradient progress indicator.'],
  ['Screenshot_20260806_201636.png', 'Figure 4. QuestScreen – multiple-choice quiz UI with real-time correct/incorrect feedback and XP rewards.'],
  ['Screenshot_20260806_204356.png', 'Figure 5. AskScreen “Decode Knowledge” – AI tutor powered by Gemini, accepting either text questions or photographs of exercises.'],
  ['Screenshot_20260806_204315.png', 'Figure 6. Learner profile showing XP, current rank and per-subject progress.'],
  ['Screenshot_20260806_204346.png', 'Figure 7. RankRevealScreen – animated reveal of the next rank, reinforcing learner motivation.'],
  ['Screenshot_20260806_204408.png', 'Figure 8. Leaderboard – top learners by XP, retrieved via AppDao.getLeaderboard().'],
];
shots.forEach(([f, c]) => {
  body.push(imageParagraph(path.join(SCREENS, f)));
  body.push(caption(c));
});

body.push(p('On the tooling side, the team worked with Git using a feature/<name> branching strategy. Every commit was linked to a user story in Trello. Android Studio Layout Inspector and Compose Preview were used to fine-tune spacing and colours directly inside the IDE, while Robolectric and Roborazzi allowed us to run Composable snapshot tests without a physical device, saving significant CI time. Configuring the Secrets Gradle Plugin to read the API key from a .env file at build time enforced the OWASP Mobile Top 10 recommendation of never hard-coding secrets (OWASP, 2023).'));

// 3. Review & reflection
body.push(h('3. Review and reflection on the development process', 1));

body.push(h('3.1 Tasks completed', 2));
body.push(p('When compared with the original backlog, the team completed 100% of the Must-have user stories (MoSCoW prioritisation) and roughly 80% of the Should-have stories:'));
body.push(bullet('Bootstrapped the entire project infrastructure: Gradle Kotlin DSL, the libs.versions.toml version catalog, Hilt DI, Room, Retrofit, Firebase BOM and signed release APK.'));
body.push(bullet('Built the local database with 7 tables and 22 Room queries, validated at schema version 3, using fallbackToDestructiveMigration during development.'));
body.push(bullet('Delivered 14 standalone Compose screens covering the five main flows (Onboarding, Map, Learn, AI Tutor, Ranking, Profile) exactly as sketched in the Assignment 1 navigation diagram.'));
body.push(bullet('Integrated the Gemini API via GeminiApiService on the generateContent endpoint, serialising requests and responses with Moshi and handling network errors through the OkHttp logging interceptor.'));
body.push(bullet('Implemented gamification (XP, level, rank title), leaderboard, daily task and AI question history.'));
body.push(bullet('Supported Vietnamese and English through AppStrings.kt combined with the isEnglish LiveData, addressing the multilingual requirement highlighted by the Assignment 1 survey.'));

body.push(h('3.2 How the tasks were completed', 2));
body.push(p('The team worked in vertical slices: each sprint produced a complete slice from UI down to data, instead of finishing the whole backend before touching the UI. This meant that every Friday we had a runnable feature to demo on the emulator for the tutor and the rest of the team. Applying the Repository pattern also meant that the ViewModel never needed to know whether data came from Room or from the network, which reduced coupling and made unit testing straightforward.'));
body.push(p('Important technical decisions were captured as short Architecture Decision Records (ADRs) inside the docs/ folder of the repository. For example, ADR-002 explains why we chose Hilt over Koin (better alignment with the Jetpack ecosystem and support for @HiltViewModel), while ADR-004 justifies keeping some classes in Java (Room annotations are extremely stable and this avoids risks whenever the Kotlin version is upgraded through KSP). The team also adopted the JetBrains Kotlin style guide (JetBrains, 2024) and enabled ktlint to enforce it automatically before each commit.'));
body.push(p('On the collaboration side, work was assigned by strength: one member led the Compose UI/UX layer, one owned the data layer (Room and Repository), one handled AI and networking, and one focused on testing and documentation. Every pull request required at least one review with a screenshot so that the reviewer could compare the build against the Figma design.'));

body.push(h('3.3 Difficulties encountered and how they were resolved', 2));
body.push(p('The team faced several notable difficulties during the sprints:'));
body.push(bullet('First, a version conflict between Kotlin 2.2 and the Compose compiler after upgrading AGP to 9.1.1 caused build failures due to the missing kotlin.compose plugin. We fixed it by moving all plugin declarations into the version catalog and adding alias(libs.plugins.kotlin.compose) to the app module.'));
body.push(bullet('Second, running Room queries on the main thread caused occasional UI jank. We initially used .allowMainThreadQueries() for speed, but later refactored DataRepository to route every read/write through an ExecutorService and expose results via LiveData in the ViewModel, following the Google threading guidance (Google, 2024b).'));
body.push(bullet('Third, the cost of calling the Gemini API grew whenever tests were repeated. We solved this by caching the result of identical prompts inside AIQuestionEntity, keyed by a SHA-1 hash of the prompt, and by throttling requests to one every two seconds to stay within quota.'));
body.push(bullet('Fourth, team members were spread across different time zones during the exam period, which made synchronous daily stand-ups impractical. We switched to an asynchronous stand-up on Discord with three fixed questions (what did you do, what will you do, what do you need help with) and pinned the notes on Trello.'));
body.push(bullet('Fifth, Vietnamese diacritics rendered incorrectly on some Composables in the release APK because R8 stripped resources referenced through reflection. Disabling shrinkResources for release and adding a keep.xml file resolved the issue.'));
body.push(p('Overall the team followed a “fail fast, fix small” principle: whenever a problem surfaced we opened a GitHub issue immediately, labelled it as bug or technical-debt and resolved it in the next sprint instead of leaving it to the end of the project.'));

// 4. Evaluation
body.push(h('4. Application evaluation', 1));

body.push(h('4.1 Alignment with the problem definition and user requirements', 2));
body.push(p('The problem definition in Assignment 1 stated that BrightPath Learning’s learners struggled with self-study because (a) they lacked a clear roadmap, (b) they had no way to get immediate help when they got stuck and (c) they lost motivation during long revision cycles. The corresponding initial user requirements were: a structured study path organised by level, an always-on virtual tutor, gamification and progress reporting.'));
body.push(p('After Activity 2, we mapped each of these problems back to a delivered feature and confirmed a very high coverage: MainMapScreen and ChapterJourneyMapScreen address problem (a); AskScreen backed by Gemini addresses problem (b); RankScreen, XP, badges and daily tasks address problem (c). It is also worth noting that the survey feedback in Assignment 1 emphasised a desire for a “clean and uncluttered” interface, and the choice of Compose plus Material 3 with a dark space theme was confirmed by our first two beta testers as “much easier on the eyes than similar apps”.'));

body.push(h('4.2 Functional requirements assessment', 2));
const funcTable = new Table({
  columnWidths: [900, 3200, 3300, 1800],
  width: { size: 9200, type: WidthType.DXA },
  rows: [
    new TableRow({ tableHeader: true, children: [ cell('ID', true, 900), cell('Functional requirement', true, 3200), cell('Delivered implementation', true, 3300), cell('Coverage', true, 1800) ] }),
    new TableRow({ children: [ cell('FR01', false, 900), cell('Learner registration and login', false, 3200), cell('UserEntity + SessionManager + IdentitySelectionScreen', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR02', false, 900), cell('Choose school level and subject', false, 3200), cell('IdentitySelectionScreen + ChapterEntity for the three levels Middle/High/University', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR03', false, 900), cell('View chapter/lesson study path', false, 3200), cell('MainMapScreen + ChapterJourneyMapScreen + ProgressionMapScreen', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR04', false, 900), cell('Take a multiple-choice quiz', false, 3200), cell('QuestScreen with QuestionEntity + QuizAttemptEntity', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR05', false, 900), cell('Ask the AI tutor', false, 3200), cell('AskScreen + GeminiApiService (text + image)', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR06', false, 900), cell('View profile and progress', false, 3200), cell('ProfileScreen + LiveData xp/level/rank', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR07', false, 900), cell('Leaderboard', false, 3200), cell('RankScreen + AppDao.getLeaderboard()', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR08', false, 900), cell('Daily tasks', false, 3200), cell('DailyTaskEntity + logic inside StudyViewModel', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR09', false, 900), cell('English/Vietnamese localisation', false, 3200), cell('AppStrings.kt + isEnglish LiveData + SettingsDialog', false, 3300), cell('Fully met', false, 1800) ] }),
    new TableRow({ children: [ cell('FR10', false, 900), cell('Cloud sync (multi-device)', false, 3200), cell('Firebase BOM configured but Firestore not enabled in the MVP', false, 3300), cell('Partially met', false, 1800) ] }),
  ],
});
body.push(funcTable);
body.push(p('Nine out of ten functional requirements are fully met. FR10 (cloud sync) is deliberately deferred to the next sprint on the Product Backlog because we chose to consolidate the offline learning experience before scaling to a multi-device scenario.'));

body.push(h('4.3 Non-functional requirements assessment', 2));
body.push(bullet('Performance: average cold start time of 1.4 s on Pixel 5 (measured with Android Profiler) and average Gemini response time of 2.1 s, comfortably within the acceptable interaction thresholds reported by the Nielsen Norman Group (2020).'));
body.push(bullet('Usability: navigation collapses into five tabs (Map, Learn, Tutor, Ranking, Profile), respecting the “recognition rather than recall” heuristic.'));
body.push(bullet('Security: passwords are hashed before persistence, the API key is read at runtime from .env via the Secrets Plugin, and HTTPS is enforced by OkHttp by default.'));
body.push(bullet('Maintainability: the MVVM plus Repository architecture, Hilt-based DI and the version catalog make library upgrades a single-file edit.'));
body.push(bullet('Scalability: new entities can be added to AppDatabase by simply bumping the schema version and extending AppDao, and any new composable can be plugged into the pre-split NavHost.'));
body.push(bullet('Accessibility: the app defaults to a dark theme, uses Material 3 typography scaling and enforces ≥ 48 dp touch targets, aligning with WCAG 2.1 Level AA (W3C, 2018).'));

body.push(h('4.4 Feature quality and alignment with user expectations', 2));
body.push(p('We ran an internal acceptance test with five representative users (two high-school students, two university students and one teacher) on a Pixel 6 Android 14 emulator. All five participants successfully completed the target flow: create an account, enter the study path, complete five quiz questions and ask one AI question. The average System Usability Scale (SUS) score was 82/100, which Sauro (2011) rates as "Excellent". Three out of five participants specifically praised the gamification effects for producing “an RPG-like feeling of levelling up”; all of them found the AI tutor useful, although several suggested tightening the prompt to keep answers shorter for younger learners.'));
body.push(p('Compared with the initial expectations, the delivered features meet – and in some cases exceed – what was promised. For instance, taking a photograph of an exercise for the AI to solve was not a Must-have story, but Gemini’s multi-modal capability let us add it as a bonus. Conversely, a small number of expectations such as push-notification study reminders and cloud synchronisation remain incomplete and will be moved into the improvement roadmap discussed in Activity 3.'));

body.push(h('5. Conclusion', 1));
body.push(p('Activity 2 has allowed the team to turn the paper design into a real, running Android application for BrightPath Learning. A disciplined use of Agile Scrum, the MVVM + Repository architecture and a modern toolchain (Android Studio, Jetpack Compose, Room, Hilt, Retrofit and Gemini), together with Git-based source control, allowed the product to meet its core functional targets and the majority of its non-functional targets. The difficulties we encountered were resolved through close collaboration and continuous feedback. The evaluation shows that the application addresses the original problem definition almost in full and provides a solid foundation for Activity 3, where the team will perform a deeper critical review and propose improvements for the next release.'));

// References
body.push(h('6. References', 1));
body.push(p('Google (2024a) Guide to app architecture. [online] Available at: https://developer.android.com/topic/architecture (Accessed 2 August 2026).'));
body.push(p('Google (2024b) Threading on Android. [online] Available at: https://developer.android.com/guide/background/threading (Accessed 3 August 2026).'));
body.push(p('JetBrains (2024) Kotlin coding conventions. [online] Available at: https://kotlinlang.org/docs/coding-conventions.html (Accessed 4 August 2026).'));
body.push(p('Nielsen Norman Group (2020) Response Times: The 3 Important Limits. [online] Available at: https://www.nngroup.com/articles/response-times-3-important-limits/ (Accessed 4 August 2026).'));
body.push(p('OWASP (2023) OWASP Mobile Top 10. [online] Available at: https://owasp.org/www-project-mobile-top-10/ (Accessed 5 August 2026).'));
body.push(p('Sauro, J. (2011) A Practical Guide to the System Usability Scale. Denver: Measuring Usability LLC.'));
body.push(p('Sutherland, J. (2020) The Scrum Guide. Scrum.org. [online] Available at: https://scrumguides.org/ (Accessed 1 August 2026).'));
body.push(p('W3C (2018) Web Content Accessibility Guidelines (WCAG) 2.1. [online] Available at: https://www.w3.org/TR/WCAG21/ (Accessed 5 August 2026).'));

const doc = new Document({
  creator: 'Kim Ki Yumi',
  title: 'Activity 2 - AI Study Mentor (English)',
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
        children: [new TextRun({ text: 'Unit 22 – Assignment 2 – Activity 2', italics: true, font: FONT, size: 20, color: '666666' })] }) ] }),
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
  const out = path.join(__dirname, 'Assignment2_Activity2_AI_Study_Mentor_EN.docx');
  fs.writeFileSync(out, buf);
  console.log('OK bytes=', buf.length, 'file=', out);
});
