#!/usr/bin/env bash
# Лаб 8: results/ дахь 4 файлыг БОДИТ `mvn test`-ээр үүсгэнэ (macOS + Linux).
# Төслийн хавтас (pom.xml-тэй) дотроос ажиллуул:
#   cd lab08-mockito && bash /зам/run-lab.sh
set -u
F=src/main/java/mn/edu/must/sqat/CourseRegistrationService.java
if [ ! -f pom.xml ] || [ ! -f "$F" ]; then
  echo "pom.xml олдсонгүй — lab08-mockito хавтас дотроос ажиллуул"; exit 1
fi
mkdir -p results
ORIG=$(mktemp)
cp "$F" "$ORIG"
restore() { cp "$ORIG" "$F"; }
trap restore EXIT

run() { mvn test 2>&1 | tee "results/$1" >/dev/null; }

mutate() { # $1 = perl орлуулалт, $2 = нэр
  perl -pi -e "$1" "$F"
  if cmp -s "$ORIG" "$F"; then echo "$2 хэрэгжсэнгүй (код таарсангүй)"; exit 1; fi
}

echo "=== Мутант 1: идэвхгүй оюутны шалгалт ==="
mutate 's|^(\s*)if \(!student\.active\(\)\) return "ERROR_INACTIVE_STUDENT";|$1// if (!student.active()) return "ERROR_INACTIVE_STUDENT";|' "Мутант 1"
run mvn-test-mutant-1.txt; restore

echo "=== Мутант 2: saveRegistration ==="
mutate 's|^(\s*)repository\.saveRegistration\(studentId, courseId\);|$1// repository.saveRegistration(studentId, courseId);|' "Мутант 2"
run mvn-test-mutant-2.txt; restore

echo "=== Мутант 3: courseId == null шалгалт ==="
mutate 's/studentId == null \|\| courseId == null/studentId == null/' "Мутант 3"
run mvn-test-mutant-3.txt; restore

echo "=== Эх код сэргээгдсэн; эцсийн ногоон ажиллуулалт ==="
run mvn-test.txt

echo; echo "---- ХАРИУ ----"
for f in mvn-test mvn-test-mutant-1 mvn-test-mutant-2 mvn-test-mutant-3; do
  echo "[$f]"
  grep 'Tests run:' results/$f.txt | tail -1
  grep -E 'BUILD (SUCCESS|FAILURE)' results/$f.txt
  grep -E 'ERROR\].*(register_|CourseRegistrationServiceTest\.)' results/$f.txt | head -4
done
echo; echo "Эх код өөрчлөгдөөгүй эсэх: git diff --stat src/main (хоосон байх ёстой)"
