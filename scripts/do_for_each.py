import os
import argparse
import re

parser=argparse.ArgumentParser()
parser.add_argument("list", type=str, help=" the list actions to be applied to ")
parser.add_argument("pattern", type=str, help=" the pattern to be used for matching an element")
parser.add_argument("action", type=str, help=" the action to be taken on each file")
args=parser.parse_args()
files=[f for f in eval(args.list) if re.match(args.pattern,f)]
for f in files:
	os.system(args.action.replace("@f",f))
