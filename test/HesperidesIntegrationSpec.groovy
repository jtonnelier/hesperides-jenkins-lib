// This file is part of the hesperides-jenkins-lib distribution.
// (https://github.com/voyages-sncf-technologies/hesperides-jenkins-lib)
// Copyright (c) 2017 VSCT.
//
// hesperides-jenkins-lib is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as
// published by the Free Software Foundation, version 3.
//
// hesperides-jenkins-lib is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import com.vsct.dt.hesperides.jenkins.pipelines.http.HttpException
import spock.lang.Shared
import spock.lang.Specification
import com.vsct.dt.hesperides.jenkins.pipelines.Hesperides
import com.vsct.dt.hesperides.jenkins.pipelines.http.HTTPBuilderRequester


// Les méthodes définies dans un trait ne seront pas exécutées comme des tests
trait Helper {

    /**
     * +1 au dernier chiffre précédé d'un '.'
     * @param version au format #.#.# ...
     * @return
     */
    def nextVersion(String version) {
        def versionDigits = version.split('\\.')
        def baseVersion = versionDigits.length > 1 ? versionDigits[0..versionDigits.length - 2].join('.') + '.' : ''
        def lastNumber = versionDigits.last() as Integer

        "${baseVersion}${lastNumber + 1}"
    }

}

class HesperidesIntegrationSpec extends Specification implements Helper {

    static final ENV = System.properties
    @Shared
    Hesperides hesperides = new Hesperides(apiRootUrl: "${ENV.HESPERIDES_HOST}:${ENV.HESPERIDES_PORT}",
                                           auth: ENV.HESPERIDES_AUTH,
                                           httpRequester: new HTTPBuilderRequester())

    static applicationName = 'app'
    static platformName = 'platform'
    static platformName2 = 'platform2'
    static moduleName = 'module'
    static secondModuleName = 'moduletwo'
    static moduleVersion = '2.0.0.0'
    static instanceName = 'instance'
    static instanceNameTwo = 'instanceTwo'
    static instanceNameThree = 'instanceThree'
    static logicGroupName = 'GROUP'
    static logicGroupNameTwo = 'CUSTOMGROUP'
    static subLogicGroup = 'TECHNO'
    static templateOne = 'templateOne.yml'
    static templateTwo = 'templateTwo.yml'
    static templateThree = 'templateThree.yml'
    static templateFour = 'templateFour.yml'
    static moduleFromDescriptorOne = 'moduleFromDescriptorOne'
    static moduleFromDescriptorTwo = 'moduleFromDescriptorTwo'
    static propertiesDiff = null
    static diffPropDisplay = ''


    def setupSpec() { // run before the first feature method
        log "${ENV.HESPERIDES_AUTH}@${ENV.HESPERIDES_HOST}:${ENV.HESPERIDES_PORT}"
        hesperides.createPlatform(app: applicationName, platform: platformName, version: '1.0.0.0')
        def infos = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
        hesperides.createModule(moduleName: moduleName, version: moduleVersion)
        hesperides.createModule(moduleName: secondModuleName, version: moduleVersion)
        hesperides.putModuleOnPlatform(app: applicationName,
                platform: platformName,
                moduleName: moduleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupName}#${subLogicGroup}")
        hesperides.putModuleOnPlatform(app: applicationName,
                platform: platformName,
                moduleName: secondModuleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupName}#${subLogicGroup}")
        hesperides.putModuleOnPlatform(app: applicationName,
                platform: platformName,
                moduleName: secondModuleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupNameTwo}#${subLogicGroup}")
        hesperides.createInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName, path: "#${logicGroupName}#${subLogicGroup}")
        hesperides.createInstance(app: applicationName, platform: platformName, moduleName: secondModuleName, instance: instanceNameTwo, path: "#${logicGroupName}#${subLogicGroup}")
        hesperides.createInstance(app: applicationName, platform: platformName, moduleName: secondModuleName, instance: instanceNameThree, path: "#${logicGroupNameTwo}#${subLogicGroup}")
    }

    def cleanupSpec() { // run after the last feature method
        hesperides.deletePlatform(app:applicationName, platform: platformName)
        hesperides.deleteModule(moduleName: moduleName, version: moduleVersion, moduleType: 'workingcopy')
        hesperides.deleteModule(moduleName: secondModuleName, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can create module and template from descriptor"() {
        when:
            hesperides.upsertFromDescriptor(descriptorPath: 'test/resources/templatesDescriptorWithModules-1-2-andTemplates-1-2-3.json', moduleVersion: moduleVersion)
        then:
            hesperides.getModule(moduleName: moduleFromDescriptorOne, version: moduleVersion, moduleType: 'workingcopy').version == moduleVersion
            hesperides.getModule(moduleName: moduleFromDescriptorTwo, version: moduleVersion, moduleType: 'workingcopy').version == moduleVersion
            hesperides.getTemplate(moduleName: moduleFromDescriptorOne, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateOne).content.trim() == 'foo={{bar}}'
            hesperides.getTemplate(moduleName: moduleFromDescriptorOne, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateTwo).content.trim() == 'bar={{foo}}'
            hesperides.getTemplate(moduleName: moduleFromDescriptorTwo, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateThree).content.trim() == 'foo={{bar}}'
        cleanup:
            hesperides.deleteModule(moduleName: moduleFromDescriptorOne, version: moduleVersion, moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: moduleFromDescriptorTwo, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can create a template in an existing module from a descriptor"() {
        when:
            hesperides.createModule(moduleName: moduleFromDescriptorOne, version: moduleVersion)
            hesperides.upsertFromDescriptor(descriptorPath: 'test/resources/templatesDescriptorWithModules-1-2-andTemplates-1-2-3.json', moduleVersion: moduleVersion)
        then:
            hesperides.getTemplate(moduleName: moduleFromDescriptorOne, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateOne).content.trim() == 'foo={{bar}}'
            hesperides.getTemplate(moduleName: moduleFromDescriptorOne, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateTwo).content.trim() == 'bar={{foo}}'
            hesperides.getTemplate(moduleName: moduleFromDescriptorTwo, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateThree).content.trim() == 'foo={{bar}}'
        cleanup:
            hesperides.deleteModule(moduleName: moduleFromDescriptorOne, version: moduleVersion, moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: moduleFromDescriptorTwo, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can update module and template from descriptor"() {
        when:
            hesperides.upsertFromDescriptor(descriptorPath: 'test/resources/templatesDescriptorWithModules-1-2-andTemplates-1-2-3.json', moduleVersion: moduleVersion)
            hesperides.upsertFromDescriptor(descriptorPath: 'test/resources/templatesDescriptorWithModules-1-2-andTemplates-3-4.json', moduleVersion: moduleVersion)
        then:
            hesperides.getTemplate(moduleName: moduleFromDescriptorOne, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateFour).content.trim() == 'bar={{foobar}}'
        cleanup:
            hesperides.deleteModule(moduleName: moduleFromDescriptorOne, version: moduleVersion, moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: moduleFromDescriptorTwo, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can delete a template from an existing module, from a descriptor"() {
        when:
            hesperides.upsertFromDescriptor(descriptorPath: 'test/resources/templatesDescriptorWithModules-1-2-andTemplates-1-2-3.json', moduleVersion: moduleVersion)
            hesperides.upsertFromDescriptor(descriptorPath: 'test/resources/templatesDescriptorWithModules-1-2-andTemplates-3-4.json', moduleVersion: moduleVersion)
        then:
            try {
                hesperides.getTemplate(moduleName: moduleFromDescriptorOne, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: templateOne)
                assert false, 'Template should not exist'
            } catch (HttpException httpException) {
                if (httpException.statusCode != 404) {
                    throw httpException
                }
                assert true
            }
        cleanup:
            hesperides.deleteModule(moduleName: moduleFromDescriptorOne, version: moduleVersion, moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: moduleFromDescriptorTwo, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can get versions of a module"() {
        when:
            def versions = hesperides.getModuleVersions(moduleName: moduleName)
        then:
            versions == [moduleVersion]
    }

    def "Can create a module"() {
        when:
            def moduleName = 'module-' + UUID.randomUUID().toString()
            def moduleVersion = '2.0.0.1'
            def module = hesperides.createModule(moduleName: moduleName, version: moduleVersion)
        then:
            module.version == moduleVersion
            module.name == moduleName
        cleanup:
            hesperides.deleteModule(moduleName: moduleName, version: moduleVersion, moduleType: 'workingcopy')
    }

    def "Can get platform info"() {
        when:
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
            log "platform info : \n ${platformInfo}"

        then:
            platformInfo['application_name'] == applicationName
            platformInfo['platform_name'] == platformName
            !platformInfo['production']
    }

    def "Can get properties for a given module template version"() {
        when:
            def props = hesperides.getModuleTemplateProperties(moduleName: moduleName, version: moduleVersion, isRelease: false)

        then:
            props['key_value_properties'] != null
            props['iterable_properties'] != null
    }

    def "Can get platform properties for a random module path"() {
        when:
            def modulePropertiesPath = hesperides.getPlatformInfo(app: applicationName, platform: platformName).modules[0].properties_path
            def props = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: modulePropertiesPath)

        then:
            props['key_value_properties'] != null
            props['iterable_properties'] != null
    }

    def "Should Add one property to the existing list when calling updatePropertiesForPlatform method"() {
        def info = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
        when:
            def modulePropertiesPath = info.modules[0].properties_path
            def props = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: modulePropertiesPath)
            props['key_value_properties'].add([
                name: "myPropertyName",
                value: "myPropertyValue"
            ])
            hesperides.updatePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: modulePropertiesPath, commitMsg: 'Update AGG properties from jenkins pipeline', properties: props, platformVid: info.version_id)
            def newProps = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: modulePropertiesPath)
        then:
            newProps['key_value_properties'].size() == 1
    }

    def "Can update new properties"() {
        when:
            def jsonProperties = """
                {
                  "${moduleName}": {
                    "LCM_vha_test_property": 42,
                    "LCM_vha_test_builtin_property": "{{hesperides.platform.name}}",
                    "iterable_properties": {
                      "LCM_vha_test_iterableProps": [
                        {
                          "LCM_vha_test_iterableProp": "TOTO"
                        }
                      ]
                    }
                  },
                  "${moduleName}#${instanceName}": {
                    "LCM_vha_test_instance_property": "hello World !"
                  },
                  "path:#${logicGroupNameTwo}#${subLogicGroup}#${secondModuleName}": {
                    "propriete_commune_secondmodule": "Canon Garrick",
                    "iterable_properties": {
                      "iterablePropsWithPath": [
                        {
                          "iterablePropWithPath": "TATA"
                        }
                      ]
                    }
                  },
                  "${secondModuleName}": {
                     "propriete_commune_secondmodule": "Kamehameha"
                  },
                  "GLOBAL": {
                    "LCM_vha_test_global_property": "Over 9000 !"
                  }
                }
                """

            File jsonFile = new File('jsonProperties')
            jsonFile.text = jsonProperties
            hesperides.updateProperties(app: applicationName,
                                        platform: platformName,
                                        jsonPropertyUpdates: jsonFile.path,
                                        commitMsg: 'hesperides-jenkins-lib Spock tests')
            jsonFile.delete()

            def modulePropertiesPath = "#${logicGroupName}#${subLogicGroup}#${moduleName}#${moduleVersion}#WORKINGCOPY"
            def moduleTwoPropertiesPath = "#${logicGroupName}#${subLogicGroup}#${secondModuleName}#${moduleVersion}#WORKINGCOPY"
            def moduleTwoPropertiesPathTwo = "#${logicGroupNameTwo}#${subLogicGroup}#${secondModuleName}#${moduleVersion}#WORKINGCOPY"
            def platformProps = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: modulePropertiesPath)
            def platformPropsModuleTwo = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: moduleTwoPropertiesPath)
            def platformPropsModuleTwoOtherPath = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: moduleTwoPropertiesPathTwo)
            def globalProps = hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName, modulePropertiesPath: '#')
            def instanceProps = hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName)

        then:
            platformProps['key_value_properties'].find { it.name == 'LCM_vha_test_property' }
            platformProps['key_value_properties'].find { it.name == 'LCM_vha_test_property' }['value'] == '42'
            platformPropsModuleTwo['key_value_properties'].find { it.name == 'propriete_commune_secondmodule'}['value'] == 'Kamehameha'
            platformProps['key_value_properties'].find {
                it.name == 'LCM_vha_test_builtin_property'
            }['value'] == '{{hesperides.platform.name}}'

            platformProps['iterable_properties'].size() > 0
            platformProps['iterable_properties'].find {
                it.name == 'LCM_vha_test_iterableProps'
            }
            platformProps['iterable_properties'].find {
                it.name == 'LCM_vha_test_iterableProps'
            }['iterable_valorisation_items'].size() > 0
            platformProps['iterable_properties'].find {
                it.name == 'LCM_vha_test_iterableProps'
            }['iterable_valorisation_items'][0]['values'].find {
                it.name == 'LCM_vha_test_iterableProp'
            }['value'] == 'TOTO'

            globalProps['key_value_properties'].find {
                it.name == 'LCM_vha_test_global_property'
            }['value'] == 'Over 9000 !'
            instanceProps['key_values'].find { it.name == 'LCM_vha_test_instance_property' }['value'] == 'hello World !'
            platformPropsModuleTwoOtherPath['key_value_properties'].find { it.name == 'propriete_commune_secondmodule'}['value'] == 'Canon Garrick'

            platformPropsModuleTwoOtherPath['iterable_properties'].size() > 0
            platformPropsModuleTwoOtherPath['iterable_properties'].find {
                it.name == 'iterablePropsWithPath'
            }['iterable_valorisation_items'][0]['values'].find {
                it.name == 'iterablePropWithPath'
            }['value'] == 'TATA'
    }

    def "Can upgrade platform version"() {
        when:
            def currentVersion = hesperides.getPlatformInfo(app: applicationName, platform: platformName)['application_version']
            def newVersion = nextVersion(currentVersion)
            hesperides.setPlatformVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: currentVersion,
                    newVersion: newVersion)
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)

        then:
            platformInfo['application_version'] == newVersion

        cleanup:
            hesperides.setPlatformVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: newVersion,
                    newVersion: currentVersion)
    }

    def "Can upgrade modules versions on a platform"() {
        when:
            def currentVersion = hesperides.getPlatformInfo(app: applicationName, platform: platformName)['modules'][0]['version']
            def newVersion = nextVersion(currentVersion)
            hesperides.setPlatformModulesVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: currentVersion,
                    newVersion: newVersion)
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)

        then:
            platformInfo['modules'].every { it['version'] == newVersion }

        cleanup:
            hesperides.setPlatformModulesVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: newVersion,
                    newVersion: currentVersion)
    }

    def "Can upgrade module version on a platform"() {
        when:
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
            def currentVersion = platformInfo['modules'][0]['version']
            def moduleName = platformInfo['modules'][0]['name']
            def newVersion = nextVersion(currentVersion)
            hesperides.setPlatformModuleVersion(app: applicationName,
                    platform: platformName,
                    newVersion: newVersion,
                    moduleName:moduleName)
            def platformInfoUpdated = hesperides.getPlatformInfo(app: applicationName, platform: platformName)

        then:
            platformInfoUpdated['modules'].find { it['name'] == moduleName && it['version'] == newVersion }

        cleanup:
            hesperides.setPlatformModuleVersion(app: applicationName,
                    platform: platformName,
                    currentVersion: newVersion,
                    newVersion: currentVersion,
                    moduleName:moduleName)
    }

    def "Can create a new module in workingcopy from scratch"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can release a module in workingcopy with same version"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.releaseModule(moduleName: 'toto', workingcopyVersion: '0.0', releaseVersion: '0.0')

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.0', moduleType: 'release')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'release')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can release a module in workingcopy with a different version"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.releaseModule(moduleName: 'toto', workingcopyVersion: '0.0', releaseVersion: '0.1')

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.1', moduleType: 'release')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.1', moduleType: 'release')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can create a new module in workingcopy from a module in workingcopy"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.createModule(moduleName: 'toto', version: '0.1', fromModule: [name: 'toto', version: '0.0', isWorkingCopy: true])

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can create a new module in workingcopy from a module in workingcopy (deprecated isWorkingcopy argument)"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.createModule(moduleName: 'toto', version: '0.1', fromModule: [name: 'toto', version: '0.0', isWorkingcopy: true])

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can create a new module in workingcopy from a released module"() {
        when:
            hesperides.createModule(moduleName: 'toto', version: '0.0')
            hesperides.releaseModule(moduleName: 'toto', workingcopyVersion: '0.0', releaseVersion: '0.0')
            hesperides.createModule(moduleName: 'toto', version: '0.1', fromModule: [name: 'toto', version: '0.0'])

        then:
            hesperides.getModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')

        cleanup:
            hesperides.deleteModule(moduleName: 'toto', version: '0.1', moduleType: 'workingcopy')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'release')
            hesperides.deleteModule(moduleName: 'toto', version: '0.0', moduleType: 'workingcopy')
    }

    def "Can download all files of an instance"() {
        when:
            hesperides.createTemplate(moduleName: moduleName, moduleVersion: moduleVersion, location: '/etc', filename: 'titi', content: 'iam=titi')
            hesperides.createTemplate(moduleName: moduleName, moduleVersion: moduleVersion, location: '/etc', filename: 'toto', content: 'iam=toto')
            def files = hesperides.getInstanceFiles(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName)
        then:
            files == [
                '/etc/titi': 'iam=titi',
                '/etc/toto': 'iam=toto',
            ]
    }

    def "Can retrieve a template with title"() {
        when:
            def template = hesperides.getTemplate(moduleName: moduleName, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: 'titi', title: 'titi')
        then:
            template.filename == 'titi'
    }

    def "Can retrieve a template without title"() {
        when:
            def template = hesperides.getTemplate(moduleName: moduleName, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: 'titi')
        then:
            template.filename == 'titi'
    }

    def "Can create a template with upsert method"() {
        when:
            hesperides.upsertTemplate(moduleName: moduleName, moduleVersion: moduleVersion, location: '/etc/test', filename: 'test_upsert', content: 'upsert content')
        then:
            hesperides.getTemplate(moduleName: moduleName, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: 'test_upsert').content == 'upsert content'

    }

    def "Can update a template with upsert method"() {
        when:
            hesperides.createTemplate(moduleName: moduleName, moduleVersion: moduleVersion, moduleType: 'workingcopy', location: '/etc/test', filename: 'test_upsert2', content: 'upsert content')
            hesperides.upsertTemplate(moduleName: moduleName, moduleVersion: moduleVersion, moduleType: 'workingcopy', location: '/etc/test', filename: 'test_upsert2', content: 'upsert content 33')
        then:
            hesperides.getTemplate(moduleName: moduleName, moduleVersion: moduleVersion, moduleType: 'workingcopy', filename: 'test_upsert2').content == 'upsert content 33'
    }

    def "Can delete an instance"() {
        setup:
            hesperides.createInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO')
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == []
        when:
            hesperides.deleteInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO')
        then:
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == null
    }

    def "Can delete all instances"() {
        setup:
            hesperides.createInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO')
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == []
        when:
            hesperides.deleteInstance(app: applicationName, platform: platformName, moduleName: moduleName, instance: '*')
        then:
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: instanceName) == null
            hesperides.getInstanceProperties(app: applicationName, platform: platformName, moduleName: moduleName, instance: 'TOTO') == null
    }

    def "Can compare a platform with itself"() {
        when:
            propertiesDiff = hesperides.getDiffProperties(
                app: applicationName,
                platform: platformName,
                modulePropertiesPath: "#${logicGroupName}#${subLogicGroup}#${moduleName}#${moduleVersion}#WORKINGCOPY"
            )
        then:
            propertiesDiff.differing == []
            propertiesDiff.only_left == []
            propertiesDiff.only_right == []
    }

    def "Can compare 2 deployed modules on different platforms"() {
        setup:
            hesperides.createPlatform(app: applicationName, platform: platformName2, version: '1.0.0.0')
            hesperides.putModuleOnPlatform(
                app: applicationName,
                platform: platformName2,
                moduleName: secondModuleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupName}#${subLogicGroup}"
            )
        when:
            propertiesDiff = hesperides.getDiffProperties(
                app: applicationName,
                platform: platformName,
                modulePropertiesPath: "#${logicGroupName}#${subLogicGroup}#${moduleName}#${moduleVersion}#WORKINGCOPY",
                toPlatform: platformName2,
                toModulePropertiesPath: "#${logicGroupName}#${subLogicGroup}#${secondModuleName}#${moduleVersion}#WORKINGCOPY")
        then:
            propertiesDiff.common == []
            propertiesDiff.differing == []
            propertiesDiff.only_left.size() == 3
            propertiesDiff.only_right == []
        cleanup:
            hesperides.deletePlatform(app: applicationName, platform: platformName2)
    }

    def "Can clean unused properties of a given module"() {
        setup:
            def platform = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
            def modulePropertiesPath = platform.modules[0].properties_path
            def newProperties = [
                iterable_properties: [],
                key_value_properties: [[
                    name: "myPropertyName",
                    value: "myPropertyValue"
                ]]
            ]
            hesperides.updatePropertiesForPlatform(app: applicationName, platform: platformName,
                                                   modulePropertiesPath: modulePropertiesPath,
                                                   commitMsg: 'Test clean unused properties',
                                                   properties: newProperties,
                                                   platformVid: platform.version_id)
        when:
            hesperides.cleanUnusedProperties(app: applicationName, platform: platformName,
                                             properties_path: modulePropertiesPath)
        then:
            hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName,
                                                      modulePropertiesPath: modulePropertiesPath).key_value_properties == []
    }

    def "Can clean unused properties of a whole platform"() {
        setup:
            def platform = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
            def modulePropertiesPath = platform.modules[0].properties_path
            def newProperties = [
                iterable_properties: [],
                key_value_properties: [[
                    name: "myPropertyName",
                    value: "myPropertyValue"
                ]]
            ]
            hesperides.updatePropertiesForPlatform(app: applicationName, platform: platformName,
                                                   modulePropertiesPath: modulePropertiesPath,
                                                   commitMsg: 'Test clean unused properties',
                                                   properties: newProperties,
                                                   platformVid: platform.version_id)
        when:
            hesperides.cleanUnusedProperties(app: applicationName, platform: platformName)
        then:
            hesperides.getModulePropertiesForPlatform(app: applicationName, platform: platformName,
                                                      modulePropertiesPath: modulePropertiesPath).key_value_properties == []
    }

    def "Create a module with a logicGroupPath starting with a #"() {
        setup:
            hesperides.createPlatform(app: applicationName, platform: platformName2, version: '1.0.0.0')
            hesperides.putModuleOnPlatform(app: applicationName,
                platform: platformName2,
                moduleName: moduleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupName}#${subLogicGroup}"
            )
        when:
            def platformInfo = hesperides.getPlatformInfo(app: applicationName, platform: platformName2)
        then:
            platformInfo['platform_name'] == platformName2
            platformInfo['application_version'] == '1.0.0.0'
            platformInfo['modules'][0]['name'] == moduleName
            platformInfo['modules'][0]['version'] == moduleVersion
            platformInfo['modules'][0]['working_copy'] == true
            platformInfo['modules'][0]['path'] == "#${logicGroupName}#${subLogicGroup}"
            platformInfo['modules'][0]['properties_path'] == "#${logicGroupName}#${subLogicGroup}#${moduleName}#${moduleVersion}#WORKINGCOPY"
        cleanup:
            hesperides.deletePlatform(app: applicationName, platform: platformName2)
    }

    def "Can display, as text, the differences of properties between 2 deployed modules on the same platform"() {
        setup:
            hesperides.createPlatform(app: applicationName, platform: platformName2, version: '1.0.0.0')
            hesperides.putModuleOnPlatform(
                app: applicationName,
                platform: platformName2,
                moduleName: moduleName,
                moduleVersion: moduleVersion,
                isWorkingCopy: true,
                logicGroupPath: "#${logicGroupName}#${subLogicGroup}")
            def modulePropertiesPath = "#${logicGroupName}#${subLogicGroup}#${moduleName}#${moduleVersion}#WORKINGCOPY"

            def props = [
                key_value_properties: [
                    [name: "myPropertyName", value: "myPropertyValue1"]
                ],
                iterable_properties: []
            ]
            def platform1Info = hesperides.getPlatformInfo(app: applicationName, platform: platformName)
            hesperides.updatePropertiesForPlatform(
                app: applicationName,
                platform: platformName,
                modulePropertiesPath: modulePropertiesPath,
                commitMsg: 'Update properties for getDiffPropertiesAsString test function PTF1',
                properties: props,
                platformVid: platform1Info.version_id)

            props = [
                key_value_properties: [
                    [name: "myPropertyName", value: "myPropertyValue2"]
                ],
                iterable_properties: []
            ]
            def platform2Info = hesperides.getPlatformInfo(app: applicationName, platform: platformName2)
            hesperides.updatePropertiesForPlatform(
                app: applicationName,
                platform: platformName2,
                modulePropertiesPath: modulePropertiesPath,
                commitMsg: 'Update properties for getDiffPropertiesAsString test function PTF2',
                properties: props,
                platformVid: platform2Info.version_id)
        when:
            diffPropDisplay = hesperides.getDiffPropertiesAsString(
                app: applicationName,
                platform: platformName,
                modulePropertiesPath: modulePropertiesPath,
                toPlatform: platformName2,
                toModulePropertiesPath: modulePropertiesPath,
                diffType: 'differing')
        then:
            diffPropDisplay == '''\
*********************************************************
      Total of items in the "differing" section : 1
*********************************************************

  =========================================================================================
|                                  REPORT DIFF PROPERTIES                                   |
| ========================================================================================= |
|    #    |      PROPERTIES      |      FINAL LEFT VALUE      |      FINAL RIGHT VALUE      |
| ========================================================================================= |
|    1    | myPropertyName       |      myPropertyValue1      |      myPropertyValue2       |
| ========================================================================================= |
'''
        cleanup:
            hesperides.deletePlatform(app: applicationName, platform: platformName2)
    }

    def log(msg) {
        System.out.println msg
    }
}
