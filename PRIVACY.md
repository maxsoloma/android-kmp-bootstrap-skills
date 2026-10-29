# Privacy and data handling

Android & KMP Bootstrap is a local, open-source development tool. It has no hosted
service, user account, credential store or analytics endpoint operated by the
project's maintainer.

The Kotlin CLI processes the project names, identifiers, choices and paths supplied
to it on the user's computer. It writes configuration, generated source files and
verification logs locally. Generation does not contact the demo network service.
Users control these files and can remove them using their normal filesystem tools.

The selected coding agent processes conversations and tool calls under that agent
provider's policies. Kotlin Toolchain, SDK tools and dependency repositories use
network access for installation and builds under their vendors' policies.

If Ktor is selected, a generated app calls its configured HTTPS endpoint when
the user presses Fetch. The default example is https://httpbin.org/uuid; replace
it for your application. The endpoint receives normal HTTP connection information.
Settings stores the example counter in the selected platform's local storage.
It is not intended for credentials or sensitive personal data.

Optional Firebase connection uses the user's authenticated Firebase tools to
list/select a project, register mobile apps and retrieve public SDK configs.
Local generation stays offline. The normalized spec embeds public Firebase SDK
identifiers for replay; private admin credentials and APNs keys are not accepted.
When a configured generated app runs, selected Firebase SDKs communicate with
Google under Firebase's policies. Analytics can collect app usage; Messaging
uses installation/device registration tokens; Remote Config fetches values when
the application invokes it. App owners control consent and service configuration.

Support is available through the repository's GitHub issues. Information posted
to a public issue is public; do not include credentials or private project data.
The software is distributed under the MIT license in LICENSE.
