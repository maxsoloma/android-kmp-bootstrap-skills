# Design

Generate fresh temporary applications with unique test application IDs. Select
Koin, applicable navigation, Ktor and Settings to exercise integrated app behavior.
Use a dedicated Android AVD and an iOS simulator with an installed runtime; use
separate test browser tabs and the generated desktop application.

For each UI: launch, increment the counter, open Details, increment there, return,
trigger a real HTTPS request and observe the response/error state, then terminate
and relaunch without clearing application data and verify the retained counter.
For web, reload and reopen the same origin. Preserve the state owner above the
back stack. Inspect actual UI through computer-use tooling and retain screenshots
or precise accessibility observations and logs. Never infer UI success from unit
tests or successful startup commands.

Use public non-sensitive test endpoints; never send credentials or user content.
Report endpoint/CORS/host constraints accurately and fix app defects demonstrated
by runtime evidence. Rebuild the JAR and run existing regressions after changes;
regenerate fresh fixtures and rerun affected UI scenarios.
