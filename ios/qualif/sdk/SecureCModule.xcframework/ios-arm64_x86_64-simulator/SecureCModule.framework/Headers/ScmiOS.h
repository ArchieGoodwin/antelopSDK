#ifndef ScmiOs_hpp
#define ScmiOs_hpp

#if defined(__cplusplus)

#include "BinaryStringLight.h"

class ScmiOS {

private:
    const char *getIdentifierForVendor();

public:
    __attribute__((visibility("default")))
    ScmiOS();

    __attribute__((visibility("default")))
    ~ScmiOS();


    __attribute__((visibility("default")))
    void initialize();

    __attribute__((visibility("default")))
    int spn(char *packageName, char *applicationDataDirectory);


    __attribute__((visibility("default")))
    BinaryStringLight *m(int commandId,
                    int messageType,
                    BinaryStringLight *payloadBsl,
                    BinaryStringLight *currentPasscodeBsl,
                    BinaryStringLight *nextPasscodeBsl,
                    BinaryStringLight *scmProtectionCodeBsl,
					int sensitiveInputJsonDataElementCount,
                    BinaryStringLight **sensitiveInputJsonData);

    __attribute__((visibility("default")))
    BinaryStringLight *p1(BinaryStringLight *cryptogram,
                                  int keyIndex,
                                  int emvMethod,
                                  int authenticationMethodType,
                                  BinaryStringLight *passcode,
                                  bool clearKey);
};

#endif /* C++ */
#endif /* ScmiOs_hpp */
