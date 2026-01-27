local cjson = require("cjson.safe")
local sha256 = require("resty.sha256")
local bit = require("bit")

local function unauthorized(message)
  ngx.status = ngx.HTTP_UNAUTHORIZED
  ngx.header["Content-Type"] = "application/json; charset=utf-8"
  ngx.say(string.format('{"error":"unauthorized","message":%q}', message or "Unauthorized"))
  return ngx.exit(ngx.HTTP_UNAUTHORIZED)
end

local function base64url_decode(input)
  if not input then
    return nil
  end
  local s = input:gsub("-", "+"):gsub("_", "/")
  local pad = #s % 4
  if pad == 2 then
    s = s .. "=="
  elseif pad == 3 then
    s = s .. "="
  elseif pad ~= 0 then
    return nil
  end
  return ngx.decode_base64(s)
end

local function base64url_encode(input_bytes)
  local b64 = ngx.encode_base64(input_bytes, true)
  return b64:gsub("+", "-"):gsub("/", "_")
end

local function constant_time_equals(a, b)
  if type(a) ~= "string" or type(b) ~= "string" then
    return false
  end
  if #a ~= #b then
    return false
  end
  local diff = 0
  for i = 1, #a do
    diff = bit.bor(diff, bit.bxor(a:byte(i), b:byte(i)))
  end
  return diff == 0
end

local function hmac_sha256(key, msg)
  local block_size = 64

  if #key > block_size then
    local sha = sha256:new()
    sha:update(key)
    key = sha:final()
  end

  if #key < block_size then
    key = key .. string.rep("\0", block_size - #key)
  end

  local ipad = {}
  local opad = {}
  for i = 1, block_size do
    local k = key:byte(i)
    ipad[i] = string.char(bit.bxor(k, 0x36))
    opad[i] = string.char(bit.bxor(k, 0x5c))
  end

  local sha_inner = sha256:new()
  sha_inner:update(table.concat(ipad) .. msg)
  local inner = sha_inner:final()

  local sha_outer = sha256:new()
  sha_outer:update(table.concat(opad) .. inner)
  return sha_outer:final()
end

local secret = os.getenv("CODEROOM_JWT_SECRET")
if not secret or secret == "" then
  return unauthorized("CODEROOM_JWT_SECRET is not configured")
end

local auth = ngx.var.http_authorization
if not auth or auth == "" then
  return unauthorized("Missing Authorization header")
end

local token = auth:match("[Bb]earer%s+(.+)")
if not token or token == "" then
  return unauthorized("Authorization header must be Bearer token")
end

local header_b64, payload_b64, signature_b64 = token:match("^([^.]+)%.([^.]+)%.([^.]+)$")
if not header_b64 or not payload_b64 or not signature_b64 then
  return unauthorized("Malformed JWT")
end

local header_json = base64url_decode(header_b64)
local payload_json = base64url_decode(payload_b64)
if not header_json or not payload_json then
  return unauthorized("Malformed JWT (base64)")
end

local header = cjson.decode(header_json)
local payload = cjson.decode(payload_json)
if not header or not payload then
  return unauthorized("Malformed JWT (json)")
end

if header.alg ~= "HS256" then
  return unauthorized("Unsupported JWT alg: " .. tostring(header.alg))
end

local digest = hmac_sha256(secret, header_b64 .. "." .. payload_b64)

local expected_sig = base64url_encode(digest)
if not constant_time_equals(signature_b64, expected_sig) then
  return unauthorized("Invalid token signature")
end

local exp = payload.exp
if exp and type(exp) == "number" and exp <= ngx.time() then
  return unauthorized("Token expired")
end

local user_id = payload.sub
if not user_id or user_id == "" then
  return unauthorized("Token missing sub")
end

local role = payload.role or ""

ngx.req.set_header("X-User-Id", user_id)
if role ~= "" then
  ngx.req.set_header("X-User-Role", role)
end

ngx.req.set_header("Authorization", auth)
  