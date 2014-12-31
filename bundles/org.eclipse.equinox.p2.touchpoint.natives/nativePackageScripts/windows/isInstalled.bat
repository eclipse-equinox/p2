:: Check if a registry key and, optionally, an attribute is available in the registry.
:: Return 0 if the key/attribute is installed, 2 otherwise.
:: Usage
::   isInstalled.bat key [attributeName attributeValue]

@echo off
SetLocal EnableExtensions

Set key=%1

:: if no attribute name is supplied, only check the key
If "%2"=="" Goto check_key



:check_attribute
Set attribute=%2
Set value=%3

:: query registry and get the second and third token, which is the type and the value
For /F "usebackq tokens=2,3" %%A In (`reg query "%key%" /v "%attribute%" 2^>nul ^| find "%attribute%"`) Do (
  Set type=%%A
  Set value_actual=%%B
)

If "REG_DWORD"=="%type%" (
  :: convert hex to int value
  Set /A value_actual=%value_actual%
)

If Not %value_actual% Equ %value% exit /B 2
exit /B 0



:check_key
reg query "%key%" >nul 2>nul
If Errorlevel 1 exit /B 2

exit /B 0
