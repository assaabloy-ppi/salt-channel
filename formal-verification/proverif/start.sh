#!/bin/bash
cat .logo
echo -e "SaltChannel v2 Symbolic Verification Battery"
echo -e ""

if [ "$(which proverif | wc -m)" -lt 8 ]; then
	echo -e "[Error] ProVerif not found in system PATH."
	echo -e "[Error] Get ProVerif from http://prosecco.gforge.inria.fr/personal/bblanche/proverif/"
else
	echo -e "[\e[1mSaltChannel\e[0m] Analyzing SaltChannel (full modes)... "
	touch results/saltChannel.txt
	proverif models/saltChannel.pv > results/saltChannel.txt
	echo -e "[\e[1mSaltChannel\e[0m] Analyzing SaltChannel (forced server auth)... "
	touch results/saltChannelServerAuth.txt
	proverif models/saltChannelServerAuth.pv > results/saltChannelServerAuth.txt
	echo -e "[\e[1mSaltChannel\e[0m] Analyzing SaltChannel (forced full auth)... "
	touch results/saltChannelFullAuth.txt
	proverif models/saltChannelFullAuth.pv > results/saltChannelFullAuth.txt
	echo -e ""
	echo -e "Results are available in the results directory."
	echo ""
	echo -e "[\e[1mSummary\e[0m] SaltChannel (full modes)"
	grep -h RESULT results/saltChannel.txt
	echo ""
	echo -e "[\e[1mSummary\e[0m] SaltChannel (forced server auth)"
	grep -h RESULT results/saltChannelServerAuth.txt
	echo ""
	echo -e "[\e[1mSummary\e[0m] SaltChannel (forced full auth)"
	grep -h RESULT results/saltChannelFullAuth.txt
	echo ""
fi
