source /etc/profile
for i in {1..100000}; do
    echo "The $i times run..."
   ./gradlew clean stest -i
  backupLog
done

