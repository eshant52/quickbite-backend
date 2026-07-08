Authorization header and JWT usage

Use Authorization: Bearer <token>

Example curl (replace <TOKEN> and URL):

curl -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     http://localhost:8080/api/v1/restaurants/me

Notes:
- Server expects the standard Bearer token in Authorization header.
- JwtUtil stores claim "role" as the enum name (e.g., "CUSTOMER", "RESTAURANT_OWNER").
- SecurityConfig maps the claim to a granted authority: ROLE_<ROLE_NAME>.
- Example: role -> "RESTAURANT_OWNER" => authority: "ROLE_RESTAURANT_OWNER".
