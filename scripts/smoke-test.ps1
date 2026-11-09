<#
  Smoke test for every endpoint of the Book Worm API. Needs the app running and the DB seeded.
  Usage:  .\scripts\smoke-test.ps1 [-BaseUrl http://localhost:8080]
  Prints PASS/FAIL per check and exits with code 1 if anything failed.
  Note: it creates a new random user and places/cancels orders, and changes stock of a few seeded books.
#>
param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
$script:pass = 0; $script:fail = 0; $script:token = $null

function Call($method, $path, $body = $null, $tok = $script:token) {
    $h = @{}
    if ($tok) { $h['Authorization'] = "Bearer $tok" }
    $p = @{ Uri = "$BaseUrl$path"; Method = $method; Headers = $h; UseBasicParsing = $true }
    if ($null -ne $body) { $p.Body = ($body | ConvertTo-Json -Depth 8); $p.ContentType = 'application/json' }
    try {
        $r = Invoke-WebRequest @p
        $json = if ($r.Content) { $r.Content | ConvertFrom-Json } else { $null }
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Body = $json }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw }
        $text = ''
        try { $text = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd() } catch {}
        $json = $null; try { $json = $text | ConvertFrom-Json } catch {}
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $json }
    }
}
function Check($name, $res, [int]$expected, [scriptblock]$extra = $null) {
    $ok = ($res.Status -eq $expected)
    if ($ok -and $extra) { try { $ok = [bool](& $extra $res.Body) } catch { $ok = $false } }
    if ($ok) { $script:pass++; Write-Host "PASS  $name ($($res.Status))" -ForegroundColor Green }
    else { $script:fail++; $msg = if ($res.Body.message) { $res.Body.message } else { '' }
           Write-Host "FAIL  $name (got $($res.Status), expected $expected) $msg" -ForegroundColor Red }
    return $res
}

$email = "smoke$([guid]::NewGuid().ToString('N').Substring(0,8))@test.com"

# ---- public catalog
$null = Check 'GET /api/home (guest)'            (Call GET '/api/home' $null $null) 200 { $args[0].bestsellers.Count -gt 0 }
$cats = Check 'GET /api/categories'              (Call GET '/api/categories' $null $null) 200 { $args[0].Count -ge 19 }
$null = Check 'GET /api/brands'                  (Call GET '/api/brands' $null $null) 200 { $args[0].Count -gt 0 }
$null = Check 'GET /api/authors'                 (Call GET '/api/authors' $null $null) 200 { $args[0].Count -gt 0 }
$null = Check 'GET /api/authors/{id}'            (Call GET '/api/authors/1' $null $null) 200 { $args[0].books.Count -gt 0 }
$null = Check 'GET /api/authors/{id} unknown'    (Call GET '/api/authors/99999' $null $null) 404
$null = Check 'GET /api/books'                   (Call GET '/api/books' $null $null) 200 { $args[0].totalItems -ge 15 }
$null = Check 'GET /api/books search q=focus'    (Call GET '/api/books?q=focus' $null $null) 200 { $args[0].items[0].title -like '*Focus*' }
$sh = Check 'GET /api/books filter+sort'         (Call GET '/api/books?format=Paperback&maxPrice=300&sort=price_asc' $null $null) 200 { $args[0].items.Count -gt 0 -and ($args[0].items | ForEach-Object { $_.price -le 300 }) -notcontains $false }
$null = Check 'GET /api/books bad sort'          (Call GET '/api/books?sort=nope' $null $null) 400
$bk = Check 'GET /api/books/{id}'                (Call GET '/api/books/1' $null $null) 200 { $args[0].book.id -eq 1 -and $args[0].relatedBooks.Count -gt 0 }
$null = Check 'GET /api/books/{id} unknown'      (Call GET '/api/books/99999' $null $null) 404
$null = Check 'GET /api/books/{id}/related'      (Call GET '/api/books/1/related' $null $null) 200 { $args[0].Count -gt 0 }
$null = Check 'GET /api/books/{id}/reviews'      (Call GET '/api/books/1/reviews' $null $null) 200
$null = Check 'POST /api/coupons/validate valid' (Call POST '/api/coupons/validate' @{ code = 'BOOK10'; orderAmount = 508 } $null) 200 { $args[0].valid -eq $true -and $args[0].discount -eq 50.8 }
$null = Check 'POST /api/coupons/validate expired' (Call POST '/api/coupons/validate' @{ code = 'EXPIRED50'; orderAmount = 508 } $null) 200 { $args[0].valid -eq $false }
$null = Check 'GET /api/shipping/rates'          (Call GET '/api/shipping/rates?pin=560001&orderAmount=300' $null $null) 200 { $args[0].charge -eq 40 }
$null = Check 'GET /api/shipping/rates bad pin'  (Call GET '/api/shipping/rates?pin=12' $null $null) 400

# ---- auth
$null = Check 'GET /api/cart without token'      (Call GET '/api/cart' $null $null) 401
$reg = Check 'POST /api/auth/register'           (Call POST '/api/auth/register' @{ name = 'Smoke Tester'; email = $email; password = 'Passw0rd!' } $null) 201 { $args[0].token.Length -gt 20 }
$null = Check 'POST /api/auth/register duplicate' (Call POST '/api/auth/register' @{ name = 'Smoke Tester'; email = $email; password = 'Passw0rd!' } $null) 409
$null = Check 'POST /api/auth/register invalid'  (Call POST '/api/auth/register' @{ name = ''; email = 'bad'; password = '1' } $null) 400
$null = Check 'POST /api/auth/login wrong password' (Call POST '/api/auth/login' @{ email = $email; password = 'wrong' } $null) 401
$login = Check 'POST /api/auth/login'            (Call POST '/api/auth/login' @{ email = $email; password = 'Passw0rd!' } $null) 200 { $args[0].user.role -eq 'CUSTOMER' }
$script:token = $login.Body.token
$null = Check 'GET /api/users/me'                (Call GET '/api/users/me') 200 { $args[0].email -eq $email }
$null = Check 'GET /api/home (logged in)'        (Call GET '/api/home') 200 { $args[0].recommended.Count -gt 0 }
$null = Check 'GET /api/recommendations'         (Call GET '/api/recommendations') 200 { $args[0].Count -gt 0 }

# ---- addresses
$addrBody = @{ firstName = 'Smoke'; lastName = 'Tester'; email = $email; phone = '+919876543210'; line1 = '1 Test Street'; line2 = 'Block B'; city = 'Pune'; pin = '411001'; state = 'Maharashtra'; country = 'India' }
$null = Check 'POST /api/users/me/addresses invalid' (Call POST '/api/users/me/addresses' @{ firstName = 'x' }) 400
$addr = Check 'POST /api/users/me/addresses'     (Call POST '/api/users/me/addresses' $addrBody) 201
$addrBody.city = 'Mumbai'
$null = Check 'PUT /api/users/me/addresses/{id}' (Call PUT "/api/users/me/addresses/$($addr.Body.id)" $addrBody) 200 { $args[0].city -eq 'Mumbai' }
$null = Check 'GET /api/users/me/addresses'      (Call GET '/api/users/me/addresses') 200 { $args[0].Count -eq 1 }

# ---- wishlist and reviews
$null = Check 'POST /api/wishlist/{bookId}'      (Call POST '/api/wishlist/7') 201 { $args[0].Count -eq 1 }
$null = Check 'GET /api/wishlist'                (Call GET '/api/wishlist') 200 { $args[0].Count -eq 1 }
$null = Check 'DELETE /api/wishlist/{bookId}'    (Call DELETE '/api/wishlist/7') 200 { $args[0].Count -eq 0 }
$null = Check 'POST /api/books/{id}/reviews'     (Call POST '/api/books/7/reviews' @{ rating = 5; comment = 'Great read' }) 201
$null = Check 'POST /api/books/{id}/reviews invalid' (Call POST '/api/books/7/reviews' @{ rating = 9 }) 400

# ---- cart (book 7 = Joy of Minimalism Rs.149, book 3 = Path to Success Rs.359)
$null = Check 'POST /api/cart/items unknown book' (Call POST '/api/cart/items' @{ bookId = 99999; quantity = 1 }) 404
$null = Check 'POST /api/cart/items over stock'  (Call POST '/api/cart/items' @{ bookId = 7; quantity = 20 }) 409
$null = Check 'POST /api/cart/items'             (Call POST '/api/cart/items' @{ bookId = 7; quantity = 1 }) 200
$null = Check 'POST /api/cart/items (2nd book)'  (Call POST '/api/cart/items' @{ bookId = 3; quantity = 1 }) 200 { $args[0].pricing.subtotal -eq 508 -and $args[0].pricing.tax -eq 60.96 -and $args[0].pricing.deliveryCharge -eq 0 }
$null = Check 'PUT /api/cart/items/{bookId}'     (Call PUT '/api/cart/items/7' @{ quantity = 2 }) 200 { $args[0].pricing.itemCount -eq 3 }
$null = Check 'GET /api/cart with coupon preview' (Call GET '/api/cart?couponCode=WELCOME100') 200 { $args[0].pricing.couponDiscount -eq 100 }
$null = Check 'DELETE /api/cart/items/{bookId}'  (Call DELETE '/api/cart/items/7') 200 { $args[0].items.Count -eq 1 }
$null = Check 'DELETE /api/cart/items/{bookId} missing' (Call DELETE '/api/cart/items/7') 404

# ---- checkout, payment
$null = Check 'POST /api/orders/checkout bad address' (Call POST '/api/orders/checkout' @{ addressId = 99999 }) 400
$ord = Check 'POST /api/orders/checkout'         (Call POST '/api/orders/checkout' @{ addressId = $addr.Body.id; couponCode = 'WELCOME100' }) 201 { $args[0].status -eq 'PENDING_PAYMENT' -and $args[0].totalAmount -eq 468.96 }
$oid = $ord.Body.id
$null = Check 'POST /api/orders/{id}/payment invalid card' (Call POST "/api/orders/$oid/payment" @{ method = 'CREDIT_CARD'; cardNumber = '123' }) 400
$decl = Check 'POST /api/orders/{id}/payment declined' (Call POST "/api/orders/$oid/payment" @{ method = 'CREDIT_CARD'; cardNumber = '4111111111110000'; nameOnCard = 'Smoke Tester'; cvv = '123'; expiry = '12/2099' }) 200 { $args[0].payment.status -eq 'FAILED' -and $args[0].order.status -eq 'PENDING_PAYMENT' }
$paid = Check 'POST /api/orders/{id}/payment'    (Call POST "/api/orders/$oid/payment" @{ method = 'UPI'; upiId = 'smoke@okbank' }) 200 { $args[0].payment.status -eq 'SUCCESS' -and $args[0].order.status -eq 'CONFIRMED' }
$null = Check 'POST /api/orders/{id}/payment again' (Call POST "/api/orders/$oid/payment" @{ method = 'UPI'; upiId = 'smoke@okbank' }) 409
$null = Check 'GET /api/cart empty after payment' (Call GET '/api/cart') 200 { $args[0].items.Count -eq 0 }
$null = Check 'GET /api/orders/{id}/payments'    (Call GET "/api/orders/$oid/payments") 200 { $args[0].Count -eq 2 }
$null = Check 'GET /api/orders/{id}'             (Call GET "/api/orders/$oid") 200 { $args[0].cancellable -eq $true }
$null = Check 'GET /api/orders'                  (Call GET '/api/orders') 200 { $args[0].Count -eq 1 }
$null = Check 'GET /api/orders/buy-again'        (Call GET '/api/orders/buy-again') 200 { $args[0].Count -eq 2 }
$null = Check 'GET /api/orders/{id}/shipment'    (Call GET "/api/orders/$oid/shipment") 200 { $args[0].status -eq 'PROCESSING' }
$null = Check 'GET /api/orders/{id} other user'  (Call GET '/api/orders/1') 404
$null = Check 'POST /api/orders/{id}/return not delivered' (Call POST "/api/orders/$oid/return") 409

# ---- admin (non-admin first)
$null = Check 'PUT admin shipment as customer'   (Call PUT "/api/admin/orders/$oid/shipment" @{ status = 'SHIPPED' }) 403
$adm = Check 'POST /api/auth/login (admin)'      (Call POST '/api/auth/login' @{ email = 'admin@bookworm.com'; password = 'Admin@123' } $null) 200 { $args[0].user.role -eq 'ADMIN' }
$a = $adm.Body.token
$null = Check 'PUT admin shipment -> SHIPPED'    (Call PUT "/api/admin/orders/$oid/shipment" @{ status = 'SHIPPED' } $a) 200
$null = Check 'PUT admin shipment bad transition' (Call PUT "/api/admin/orders/$oid/shipment" @{ status = 'SHIPPED' } $a) 409
$null = Check 'POST /api/orders/{id}/cancel shipped' (Call POST "/api/orders/$oid/cancel") 409
$null = Check 'PUT admin shipment -> DELIVERED'  (Call PUT "/api/admin/orders/$oid/shipment" @{ status = 'DELIVERED' } $a) 200
$null = Check 'POST /api/orders/{id}/return'     (Call POST "/api/orders/$oid/return") 200 { $args[0].status -eq 'RETURNED' }

$cat = Check 'POST /api/admin/categories'        (Call POST '/api/admin/categories' @{ name = "Smoke Cat $email" } $a) 201
$null = Check 'POST /api/admin/categories duplicate' (Call POST '/api/admin/categories' @{ name = "Smoke Cat $email" } $a) 409
$pub = Check 'POST /api/admin/brands'            (Call POST '/api/admin/brands' @{ name = "Smoke Pub $email" } $a) 201
$aut = Check 'POST /api/admin/authors'           (Call POST '/api/admin/authors' @{ name = "Smoke Author $email"; bio = 'Test' } $a) 201
$isbn = '978' + (Get-Random -Minimum 1000000000 -Maximum 1999999999)
$bookReq = @{ title = 'Smoke Book'; description = 'Created by smoke test'; isbn = $isbn; authorId = $aut.Body.id; publisherId = $pub.Body.id; categoryId = $cat.Body.id; format = 'eBook'; language = 'English'; price = 99.5; stock = 5; deliveryDays = 0 }
$nb = Check 'POST /api/admin/books'              (Call POST '/api/admin/books' $bookReq $a) 201
$null = Check 'POST /api/admin/books duplicate ISBN' (Call POST '/api/admin/books' $bookReq $a) 409
$bookReq.price = 120
$null = Check 'PUT /api/admin/books/{id}'        (Call PUT "/api/admin/books/$($nb.Body.id)" $bookReq $a) 200 { $args[0].price -eq 120 }
$null = Check 'DELETE /api/admin/books/{id}'     (Call DELETE "/api/admin/books/$($nb.Body.id)" $null $a) 204

# ---- second order: cancel within 48h, then address delete and logout
$null = Call POST '/api/cart/items' @{ bookId = 7; quantity = 1 }
$o2 = Check 'POST /api/orders/checkout (2nd)'    (Call POST '/api/orders/checkout' @{ addressId = $addr.Body.id }) 201
$null = Check 'POST /api/orders/{id}/payment wallet' (Call POST "/api/orders/$($o2.Body.id)/payment" @{ method = 'WALLET'; walletId = 'w-1' }) 200
$null = Check 'POST /api/orders/{id}/cancel'     (Call POST "/api/orders/$($o2.Body.id)/cancel") 200 { $args[0].status -eq 'CANCELLED' }
$null = Check 'DELETE /api/users/me/addresses/{id}' (Call DELETE "/api/users/me/addresses/$($addr.Body.id)") 204
$null = Check 'POST /api/auth/logout'            (Call POST '/api/auth/logout') 204
$null = Check 'GET /api/users/me after logout'   (Call GET '/api/users/me') 401

Write-Host ''
Write-Host "Passed: $script:pass   Failed: $script:fail" -ForegroundColor $(if ($script:fail -eq 0) { 'Green' } else { 'Red' })
if ($script:fail -gt 0) { exit 1 }
