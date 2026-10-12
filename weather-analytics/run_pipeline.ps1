<#
  End-to-end PostgreSQL <-> Hadoop MapReduce weather pipeline.

  Prerequisite: EcoNet backend running (cd ..\econet-api; docker compose up --build).

  Usage (from the weather-analytics folder):
    .\run_pipeline.ps1                         # MapReduce via local simulator (no Hadoop needed)
    .\run_pipeline.ps1 -Mode hadoop            # MapReduce on a running Hadoop/HDFS (start-dfs.cmd first)
    .\run_pipeline.ps1 -Start 2026-04-01 -End 2026-09-30 -ApiBase http://localhost:8081
#>
param(
    [string]$Start = "2026-04-01",
    [string]$End = "2026-09-30",
    [string]$ApiBase = "http://localhost:8081",
    [ValidateSet("local", "hadoop")]
    [string]$Mode = "local"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$api = "$ApiBase/api/weather/pipeline"

Write-Host "`n[1/5] Backfilling real Vellore weather from Open-Meteo into PostgreSQL (weather_daily)..." -ForegroundColor Cyan
$r = Invoke-RestMethod -Method Post -Uri "$api/backfill?start=$Start&end=$End"
Write-Host ("      inserted={0} updated={1} skipped={2}" -f $r.data.inserted, $r.data.updated, $r.data.skippedMissing)

Write-Host "[2/5] Exporting weather_daily -> data\weather_data.dat" -ForegroundColor Cyan
Invoke-WebRequest -Uri "$api/export?start=$Start&end=$End" -OutFile "data\weather_data.dat" -UseBasicParsing
Write-Host ("      {0} lines" -f (Get-Content "data\weather_data.dat").Count)

Write-Host "[3/5] Running MapReduce ($Mode)..." -ForegroundColor Cyan
if ($Mode -eq "local") {
    New-Item -ItemType Directory -Force build | Out-Null
    javac -d build src\WeatherAggregate.java src\WeatherLocalSimulator.java
    java -cp build WeatherLocalSimulator data\weather_data.dat mapreduce_output
} else {
    hdfs dfs -mkdir -p /weather_input
    hdfs dfs -put -f data\weather_data.dat /weather_input/
    hdfs dfs -rm -r -f /weather_output
    hadoop jar weather.jar WeatherDriver /weather_input/weather_data.dat /weather_output
    New-Item -ItemType Directory -Force mapreduce_output | Out-Null
    hdfs dfs -cat /weather_output/part-r-00000 | Set-Content -Encoding ascii "mapreduce_output\part-r-00000"
}

Write-Host "[4/5] Importing part-r-00000 into PostgreSQL (weather_monthly_summary)..." -ForegroundColor Cyan
$body = Get-Content "mapreduce_output\part-r-00000" -Raw
$r = Invoke-RestMethod -Method Post -Uri "$api/import" -ContentType "text/plain" -Body $body
Write-Host "      $($r.message)"

Write-Host "[5/5] Verifying Hadoop results against SQL GROUP BY ROLLUP..." -ForegroundColor Cyan
$r = Invoke-RestMethod -Uri "$api/verify?start=$Start&end=$End"
$r.data.comparisons | ForEach-Object {
    [pscustomobject]@{
        Period      = $_.periodKey
        Days        = $_.sql.days
        HadoopTemp  = $_.hadoop.avgTemp
        SqlTemp     = $_.sql.avgTemp
        HadoopWind  = $_.hadoop.meanWindDir
        SqlWind     = $_.sql.meanWindDir
        Match       = $_.match
    }
} | Format-Table -AutoSize

if ($r.success) { Write-Host $r.message -ForegroundColor Green } else { Write-Host $r.message -ForegroundColor Red; exit 1 }
