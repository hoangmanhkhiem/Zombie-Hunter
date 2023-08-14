all: guard-ANDROID_HOME
	$(ANDROID_HOME)/tools/android update lib-project --path libs/google-play-services_lib/
	ant clean
	ant debug install

clean: guard-ANDROID_HOME
	ant clean

guard-%:
	@ if [ "${${*}}" == "" ]; then \
		echo "Environment variable $* not set"; \
		exit 1; \
	fi
