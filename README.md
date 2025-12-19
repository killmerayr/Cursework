# rating system

система для учета оценок. java + postgres.

## как затестить (qr коды ведут сюда)

просто качай зипку под свою систему, распаковывай и запускай. ничего настраивать не надо, база уже в облаке.

* **windows:** [скачать zip](https://github.com/killmerayr/Cursework/releases/download/latest/RatingSystem-windows-latest.zip)
* **macos:** [скачать zip](https://github.com/killmerayr/Cursework/releases/download/latest/RatingSystem-macos-latest.zip)
* **linux:** [скачать zip](https://github.com/killmerayr/Cursework/releases/download/latest/RatingSystem-ubuntu-latest.zip)

## че по чем

* **база:** все крутится на удаленке. коннект автоматический.
* **безопасность:** пароли зашифрованы (bcrypt), а у приложения нет прав на удаление данных из базы (защита от дурака).
* **отчеты:** можно выгружать в pdf и загружать их обратно — прила сама все распарсит.

## как собрать самому

если хочешь покопаться в коде:
1. нужна java 21 и maven.
2. `./run build` — соберет проект.
3. `./run app` — запустит.

база поднимется сама если есть докер, но по дефолту все ломится на сервак.
