rmdir /s /q temporary
mkdir temporary
del pack.jar
"C:\Program Files\7-Zip\7z.exe" x .\target\*.jar -o.\temporary\

"C:\Program Files\7-Zip\7z.exe" x .\branch_1_14\target\branch_1_14-r1.jar -o.\temporary\ -y
"C:\Program Files\7-Zip\7z.exe" x .\branch_1_15\target\branch_1_15-r1.jar -o.\temporary\ -y
"C:\Program Files\7-Zip\7z.exe" x .\branch_1_16\target\branch_1_16-r3.jar -o.\temporary\ -y
"C:\Program Files\7-Zip\7z.exe" x .\branch_1_17\target\branch_1_17-r1.jar -o.\temporary\ -y
"C:\Program Files\7-Zip\7z.exe" x .\branch_1_18\target\branch_1_18-r1.jar -o.\temporary\ -y
"C:\Program Files\7-Zip\7z.exe" x .\branch_1_19\target\branch_1_19-r1.jar -o.\temporary\ -y

"C:\Program Files\7-Zip\7z.exe" a -aou pack.jar .\temporary\*
rmdir /s /q temporary