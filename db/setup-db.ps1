<#
  Creates the bookstore database, schema and seed data.
  Usage:   .\db\setup-db.ps1
  You will be prompted for the PostgreSQL superuser (postgres) password, or set $env:PGPASSWORD first.
#>
param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$PgHost = 'localhost',
    [int]$PgPort = 5432,
    [string]$SuperUser = 'postgres'
)
$ErrorActionPreference = 'Stop'
$psql = Join-Path $PgBin 'psql.exe'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not $env:PGPASSWORD) {
    $secure = Read-Host "Password for PostgreSQL user '$SuperUser'" -AsSecureString
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}
$superPassword = $env:PGPASSWORD

function Run-Psql($user, $password, $db, $file) {
    $env:PGPASSWORD = $password
    & $psql -h $PgHost -p $PgPort -U $user -d $db -v ON_ERROR_STOP=1 -q -f $file
    if ($LASTEXITCODE -ne 0) { throw "psql failed for $file" }
}

Write-Host '1/3 Creating role and database...'
Run-Psql $SuperUser $superPassword 'postgres' (Join-Path $here '00_create_database.sql')
Write-Host '2/3 Creating schema...'
Run-Psql 'bookstore_user' 'bookstore_pass' 'bookstore' (Join-Path $here '01_schema.sql')
Write-Host '3/3 Loading seed data...'
Run-Psql 'bookstore_user' 'bookstore_pass' 'bookstore' (Join-Path $here '02_seed.sql')
Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
Write-Host 'Done. Database "bookstore" is ready (user bookstore_user / bookstore_pass).'
