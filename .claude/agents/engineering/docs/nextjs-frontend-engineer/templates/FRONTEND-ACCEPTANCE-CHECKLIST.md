# Frontend Acceptance Checklist

## Behavior

- [ ] Approved user journey implemented
- [ ] Loading state
- [ ] Empty state
- [ ] Success state
- [ ] Validation errors
- [ ] Server errors
- [ ] Unauthorized and forbidden behavior
- [ ] Not-found behavior
- [ ] Retry or recovery
- [ ] Duplicate submissions prevented
- [ ] Direct-link and refresh behavior

## API contract

- [ ] Endpoint and method verified
- [ ] Request type verified
- [ ] Response type verified
- [ ] Nullability verified
- [ ] Validation verified
- [ ] Status codes verified
- [ ] Error payload verified
- [ ] Pagination and filtering verified
- [ ] Authentication verified
- [ ] Backend authorization exists

## Accessibility

- [ ] Semantic landmarks
- [ ] Heading hierarchy
- [ ] Accessible names
- [ ] Form labels
- [ ] Keyboard access
- [ ] Visible focus
- [ ] Focus management
- [ ] Error announcements
- [ ] Status announcements
- [ ] Dialog semantics
- [ ] No color-only meaning
- [ ] Reduced motion
- [ ] Image alternatives
- [ ] Table semantics
- [ ] Touch targets

## Responsive behavior

- [ ] Small mobile
- [ ] Large mobile
- [ ] Tablet
- [ ] Desktop
- [ ] Long text
- [ ] Large lists
- [ ] Zoom
- [ ] No unintended overflow

## Quality

- [ ] Type-check passes
- [ ] Lint passes
- [ ] Unit/component tests pass
- [ ] Critical E2E tests pass
- [ ] Production build passes
- [ ] No broad suppressions
- [ ] No accidental dependencies
- [ ] No backend or migration changes
- [ ] No secrets exposed
- [ ] Final diff is focused
