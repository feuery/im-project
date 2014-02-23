unamestr=`uname`


if [[ "$unamestr" == "Darwin" ]]; then
    echo Oletettaan olevamme Macillä espoossa...
    curl -X get http://192.168.0.20:5000/login/$1/testisalasana
else
    echo Oletetaan olevamme himassa pöytäkoneella
    curl -X get http://192.168.0.18:5000/login/$1/testisalasana
fi
