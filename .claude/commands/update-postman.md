# Update Postman Collection

Review and update the BrewDeck Postman collection.

## Location

```text
docs/api/postman/brewdeck.postman_collection.json
docs/api/postman/brewdeck.local.postman_environment.json
```

## Checklist

Check that:

- Endpoints match the current Spring Boot controllers. 
- Collection GET endpoints support:
  - `page`
  - `size`
  - `sort`
- IDs use Long variables, not `{{$guid}}`:
  - `{{coffeeId}}`
  - `{{methodId}}`
  - `{{recipeId}}`
  - `{{sessionId}}`
- Favorite endpoints use:
  - `PATCH /api/recipes/{id}/favorite`
  - `PATCH /api/recipes/{id}/unfavorite`
- Request bodies match current request records:
  - `CoffeeRequest`
  - `BrewMethodRequest`
  - `RecipeRequest`
  - `BrewSessionRequest`
- Validation examples are included where useful. 
- Postman tests/scripts store created IDs into environment variables when possible. 
- No real secrets, tokens, usernames, or passwords are committed.
- `baseURL` is managed through the Postman environment. 
- The local environment file points to `http://localhost:8080`.

## Output

After reviewing, provide:

  1.  Summary of changes.
  2.  Any endpoint mismatches found.
  3.  Any missing examples.
  4.  Updated files if changes are needed.
  5.  Suggested Conventional Commit message.