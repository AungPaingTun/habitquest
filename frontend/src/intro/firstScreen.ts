// Remembers whether this visit has already shown a page other than the login screen.
// The panda intro plays only when the login page is the first screen of a visit (opening the link, or being
// sent to login), not when coming back from the sign-up page or after logging out.
// It lives in memory, so a full page reload counts as a new visit.

let otherScreenShown = false

/** Call when a page other than the first login screen is shown. */
export function markOtherScreenShown() {
  otherScreenShown = true
}

/** True while nothing but the login screen has been shown in this visit. */
export function isFirstScreen() {
  return !otherScreenShown
}
