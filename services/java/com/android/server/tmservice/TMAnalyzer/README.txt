== Usage ==
1. To parse execution traces from a file and remove noise from branch choices and output locations.

python TMAnalyzer.py -v data/test1.txt

2. To examine/compares execution traces incurred by different inputs which excercise the same branch choices.

python TMAnalyzer.py data/test1.txt data/test2.txt
python TMAnalyzer.py data/test1.txt data/test3.txt
python TMAnalyzer.py data/test1.txt data/test4.txt


