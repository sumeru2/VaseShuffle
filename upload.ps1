git init
# Создание случайного сообщения коммита
$randomCommitMessage = "Commit_$((Get-Random -Minimum 1000 -Maximum 9999))"

# Добавление всех файлов к индексу
git add --all

# Коммит с случайным сообщением
git commit -m $randomCommitMessage
git branch -M main


git remote add origin https://github.com/sumeru2/VaseShuffle.git

# Отправка изменений на удаленный репозиторий
git push -u origin main