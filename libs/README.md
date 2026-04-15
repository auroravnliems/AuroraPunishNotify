# libs/

Đặt các file jar local vào đây trước khi build:

- `CMI.jar` — plugin CMI (v9.8.4.2+)
- `CMILib.jar` — thư viện CMILib (v1.5.8.2+)

Gradle sẽ tự pick up tất cả `*.jar` trong thư mục này qua `compileOnly fileTree(dir: 'libs', include: '*.jar')`.
