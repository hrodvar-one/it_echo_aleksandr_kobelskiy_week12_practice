# REST API приложение "week12_practice"

## Оглавление
[1. Описание проекта](#описание-проекта)<br>
[2. Запуск проекта](#запуск-проекта)  
[3. Виды запросов к API через Postman](#виды-запросов-к-api-через-postman)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[- Auth_Collection](#auth_collection)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[- Users_Collection](#users_collection)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[- Events_Collection](#events_collection)<br>
&nbsp;&nbsp;&nbsp;&nbsp;[- Files_Collection](#files_collection)<br>
[4. UML диаграммы](#uml-диаграммы)

### Описание проекта
REST API приложение позволяет взаимодействовать с файловым хранилищем Yandex Cloud S3 и предоставляет возможность получать доступ<br>
к файлам и истории загрузок.<br>
Логика безопасности реализована средствами JWT токена.<br>
Приложение докеризировано и готово к развёртыванию в виде Docker контейнера.<br>
Приложение тестируется unit- и интеграционными тестами.<br>
Взаимодействие с пользователем реализовано с помощью Postman.<br>

1. Основные сущности используемые в приложении:
    - User - пользователь приложения.<br>
      по умолчанию при старте приложения создаётся учётная запись root с ролью ADMIN.
    - Event - событие загрузки либо удаления файла
    - File - сам файл который мы загружаем или скачиваем
2. Уровни доступа к основным эндпоинтам REST API реализованы через роли пользователя:
    - ADMIN - полный доступ к приложению
    - MODERATOR - права USER 
      + чтение всех User 
      + чтение/изменение/удаление всех Events
      + чтение/изменение/удаление всех Files
    - USER - только чтение всех своих данных + загрузка файлов для себя.

### Запуск проекта

* Запустите Docker и проверьте что интернет соединение доступно.
* Для запуска приложения скачайте проект в нужную директорию:
  ```
  git clone https://gitlab.leantech.ai/javateaml-akobelskiy/it_echo_aleksandr_kobelskiy_week12_practice.git
  ```
* Перейдите в директорию склонированного проекта и в командной строке введите команду:
  ```
  docker-compose up --build
  ```
* Начнётся процесс сборки и развёртывания Docker контейнера приложения. 
  Этот процесс обычно длится несколько минут.
* Приложение будет запущено по адресу:
  ```
  http://localhost:8787/
  ```
* Дальнейшее взаимодействие осуществляется через Postman.



### Виды запросов к API через Postman

Примеры запросов:

### Auth_Collection

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: cornflowerblue;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    GET
</div><br> 
(GET_AUTH_INFO)

  ```
  http://localhost:8787/api/v1/auth/info
  ```
такой запрос вернёт информацию о пользователе под чьим токеном этот запрос отправлен и возвращает данные пользователя в JSON формате:
  ```
  {
    "id": 3,
    "username": "usertest",
    "password": "kkXcf8qq8QZCv3dddrWJsNNhocx7aEyHzocdX8=",
    "role": "MODERATOR",
    "first_name": "Victor",
    "last_name": "Victorov",
    "status": "ACTIVE",
    "created_at": "2024-12-07T11:29:01",
    "updated_at": "2024-12-07T11:39:40"
  }
  ```

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: #49CC90;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    POST
</div><br> 
(POST_AUTH_REGISTER)

  ```
  http://localhost:8787/api/v1/auth/register
  ```
в тело запроса нужно передать в JSON формате параметры пользователя которого надо зарегистрировать:

  ```
  {
    "username": "avalera",
    "password": "test",
    "first_name": "Valera",
    "last_name": "Abdulov"
  }
  ```
такой запрос вернёт из БД информацию о зарегистрированном пользователе в JSON формате:
  ```
  {
    "id": 2,
    "username": "avalera",
    "password": "kkXcf8q568QZCvnj6q3VRhJwAJ764Nhocx7aEyHzocdX8=",
    "role": "USER",
    "first_name": "Valera",
    "last_name": "Abdulov",
    "status": "ACTIVE",
    "created_at": "2024-12-07T11:29:00.65233473",
    "updated_at": "2024-12-07T11:29:00.652349809"
  }
  ```


<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: #49CC90;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    POST
</div><br> 
(POST_AUTH_LOGIN)

  ```
  http://localhost:8787/api/v1/auth/login
  ```

в тело запроса нужно передать логин и пароль пользователя в JSON формате:

  ```
  {
    "username": "avalera",
    "password": "test"
  }
  ```
такой запрос вернёт 200 статус и информацию о залогиненном пользователе в JSON формате:
  ```
  {
    "user_id": 2,
    "token": "eyJhbGciOi45UzI1NiJ9.eyJyb2xlIjoiTU9ERVJBVE9SIiwidXNlcm5hbWUiOiJha29iZWx54XNzIjoiYWtvYmVsc2tpeSIsInN1YiI6IjIiLCJpYXQiOjE3MzM1NzE2MDcsImp0aSI6ImEyNjg3MTE2LWIwMjItNDMzYy1hNDQzLWU2YzIzYjZmMTIzZSIsImV4cCI6MTczMzU3NTIwN30.UcsHjzwSw0LEhNH-e3ene0sALK6sf60MEPA46qPoTT0",
    "issued_at": "2024-12-07T11:40:07.434+00:00",
    "expires_at": "2024-12-07T12:40:07.434+00:00"
  }
  ```


### Users_Collection

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: cornflowerblue;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    GET
</div><br> 
(GET_ALL_USERS)

  ```
  http://localhost:8787/api/v1/users
  ```
в поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос. Если пользователь имеет роль ADMIN или MODERATOR, то такой запрос вернёт из БД всеx User в JSON формате<br>
иначе вернёт 403 Forbidden и текст Access Denied:
  ```
  [
    {
        "id": 1,
        "username": "root",
        "password": "5KxMPdnDmXTsUCr3jk7ncRrCihqsdfsgB9z/Knp7c8bU=",
        "role": "ADMIN",
        "firstName": "System",
        "lastName": "Administrator",
        "status": "ACTIVE",
        "createdAt": "2024-12-07T15:06:45",
        "updatedAt": "2024-12-07T15:06:45",
        "events": null
    },
    {
        "id": 2,
        "username": "test",
        "password": "kkXcfghf8qq8QZCvwAJsNNhocx7aEyHzocdX8=",
        "role": "USER",
        "firstName": "Test",
        "lastName": "Testovich",
        "status": "ACTIVE",
        "createdAt": "2024-12-07T15:07:21",
        "updatedAt": "2024-12-07T15:07:21",
        "events": null
    }
  ]
  ```

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: cornflowerblue;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    GET
</div><br> 
(GET_USER_BY_ID)

  ```
  http://localhost:8787/api/v1/users/2
  ```
в поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос. Если пользователь имеет роль ADMIN , то такой запрос вернёт из БД данные пользователя в JSON формате<br>
у которого id равен 2 иначе вернёт 403 Forbidden и текст Access Denied:
  ```
  {
    "id": 2,
    "username": "test",
    "password": "kkXcfwAJsNNhocx7aEyHzocdX8=",
    "role": "USER",
    "firstName": "Test",
    "lastName": "Testovich",
    "status": "ACTIVE",
    "createdAt": "2024-12-07T15:07:21",
    "updatedAt": "2024-12-07T15:07:21",
    "events": null
  }
  ```

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: #FCA130;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    PUT
</div><br> 
(PUT_UPDATE_USER)

  ```
  http://localhost:8787/api/v1/users/2
  ```
В конце строки запроса передаётся id пользователя информацию которого вы хотите обновить.
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос. 
В тело запроса нужно передать параметры сущности которую мы хотим обновить в JSON формате:

  ```
  {
    "firstName": "VICTOR",
    "lastName": "VICTOROV",
    "role": "MODERATOR"
  }
  ```

Если пользователь имеет роль ADMIN , то такой запрос вернёт из БД данные обновлённого пользователя в JSON<br>
формате у которого id равен 2 иначе вернёт 403 Forbidden и текст Access Denied:

  ```
  {
    "id": 2,
    "username": "test",
    "password": "kkXj6qnVRhJwAJsNNhocx7aEyHzocdX8=",
    "role": "MODERATOR",
    "firstName": "VICTOR",
    "lastName": "VICTOROV",
    "status": "ACTIVE",
    "createdAt": "2024-12-07T11:29:01",
    "updatedAt": "2024-12-07T11:29:01",
    "events": null
}
  ```

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: #F93E3E;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    DELETE
</div><br> 
(DELETE_USER_BY_ID)

  ```
  http://localhost:8787/api/v1/users/3
  ```
В конце строки запроса передаётся id пользователя которого вы хотите удалить.
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос.
Если пользователь выполняющий запрос имеет роль ADMIN, и запрос пройдёт успешно, то такой запрос вернёт 204 статус<br>
иначе если пользователя с таким id нет или этот пользователь имеет статус DELETED вернёт 404 Not Found.
Если пользователь выполняющий запрос не имеет роль ADMIN, то запрос вернёт статус 403 Forbidden и текст Access Denied

***

### Events_Collection

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: cornflowerblue;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    GET
</div><br> 
(GET_ALL_EVENTS)

  ```
  http://localhost:8787/api/v1/events
  ```
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос.
Если пользователь выполняющий запрос имеет роль ADMIN, то такой запрос вернёт 200 статус и список всех Events<br>
в JSON формате:
  ```
  [
    {
        "id": 1,
        "userId": 4,
        "fileId": 1,
        "status": "ACTIVE"
    },
    {
        "id": 2,
        "userId": 1,
        "fileId": 1,
        "status": "DELETED"
    },
    {
        "id": 3,
        "userId": 1,
        "fileId": 2,
        "status": "ACTIVE"
    },
    {
        "id": 4,
        "userId": 1,
        "fileId": 2,
        "status": "DELETED"
    },
    {
        "id": 5,
        "userId": 1,
        "fileId": 3,
        "status": "ACTIVE"
    }
  ]
  ```
Если пользователь выполняющий запрос не имеет роль ADMIN, то запрос вернёт статус 403 Forbidden и текст Access Denied.


***

### Files_Collection

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: cornflowerblue;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    GET
</div><br> 
(GET_ALL_FILES_LOCATIONS)

  ```
  http://localhost:8787/api/v1/files/locations
  ```
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос.
Если пользователь выполняющий запрос имеет роль ADMIN или MODERATOR, то такой запрос вернёт из БД все ссылки на файлы<br>
в JSON формате загруженные в хранилище:
  ```
  [
    "https://storage.yandexcloud.net/javateam/victor/file_user.txt",
    "https://storage.yandexcloud.net/javateam/victor/file_moderator.txt"
  ]
  ```

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: cornflowerblue;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    GET
</div><br> 
(GET_FILE_BY_ID)

  ```
  http://localhost:8787/api/v1/files/3
  ```
В конце строки запроса передаётся id файла ссылку на который вы хотите узнать.
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос.
Если пользователь выполняющий запрос имеет роль ADMIN или MODERATOR, то такой запрос вернёт 200 статус<br>
и ссылку на файл:
  ```
  https://storage.yandexcloud.net/javateam/victor/file_moderator.txt
  ```
иначе если файла с таким id нет или этот файл имеет статус DELETED вернёт 404 Not Found и текст File not found.
Если пользователь выполняющий запрос не имеет роль ADMIN или MODERATOR, то запрос вернёт статус 403 Forbidden и текст Access Denied.


<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: #49CC90;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    POST
</div><br> 
(POST_SAVE_FILE_AND_ADD_EVENT)

  ```
  http://localhost:8787/api/v1/files
  ```
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос.
В теле запроса надо выбрать form-data и прикрепить файл.
Такой запрос при успешном выполнении вернёт 200 статус и ссылку на загруженный файл:
  ```
  File uploaded successfully: https://storage.yandexcloud.net/javateam/volodimir/file_moderator.txt
  ```
Если имя загружаемого файла совпадёт с файлом в хранилище, то вернёт 500 статус и сообщение:
  ```
  File with name 'file_moderator.txt' already exists in folder 'volodimir'
  ```

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: #FCA130;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    PUT
</div><br> 
(PUT_UPDATE_FILE)

  ```
  http://localhost:8787/api/v1/files/2
  ```
В конце строки запроса передаётся id файла который вы хотите обновить.
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос.
В теле запроса надо выбрать form-data и прикрепить файл.
Если пользователь выполняющий запрос имеет роль ADMIN или MODERATOR, то такой запрос вернёт 200 статус<br>
и сообщение:
  ```
  File successfully updated
  ```
Если пользователь выполняющий запрос имеет роль USER, то запрос вернёт статус 403 Forbidden и текст Access Denied.

<div style="color: white;
            font-weight: bold;
            text-align-last: center;
            background-color: #F93E3E;
            width: 90px; padding: 10px;
            border-radius: 5px;
            text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);">
    DELETE
</div><br> 
(DELETE_FILE_BY_ID)

  ```
  http://localhost:8787/api/v1/files/5
  ```
В конце строки запроса передаётся id файла который вы хотите обновить.
В поле Bearer Token данного запроса нужно передать токен залогиненного пользователя под которым вы хотите выполнить<br>
данный запрос.
Если пользователь выполняющий запрос имеет роль ADMIN или MODERATOR, то такой запрос вернёт 200 статус<br>
и сообщение:
  ```
  File successfully deleted
  ```
Если файл с таким id не найден, то запрос вернёт 404 Not Found и сообщение:
  ```
  File with this id not found
  ```
Если пользователь выполняющий запрос имеет роль USER, то запрос вернёт статус 403 Forbidden и текст Access Denied.

***

### UML диаграммы

![UML_class_diagram.svg](/img/UML_class_diagram.svg)
![UML_sequence_diagram.png](/img/UML_sequence_diagram.png)
![UML_component_diagram.png](/img/UML_component_diagram.png)<br>