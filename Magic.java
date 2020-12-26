package com.demo;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Magic {

    private static final String PACKAGE_NAME = MethodHandles.lookup().lookupClass().getPackage().getName();
    private static final String DOMAIN_PACKAGE_NAME = PACKAGE_NAME+".domain";

    public static void main(String[] args) {

        generateProjectedArchByEntities();

    }

    private static void generateProjectedArchByEntities(){

//        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);

        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        final Set<BeanDefinition> classes = provider.findCandidateComponents( DOMAIN_PACKAGE_NAME );

        try {
            for (BeanDefinition bean: classes) {
                Class<?> clazz =  Class.forName(bean.getBeanClassName());
                generateClasses( clazz );
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();

        }


    }

    // TODO: Check Enum s in Entity !!!!
    // Refactor "generateClasses" method   !!!
    // TODO: Check sub directory in DOMAIN package
    // TODO: Use ENUM for ppackage names
    // TODO: Write a nw function for polural field name
    private static <T> void generateClasses(Class<T> clazz ) {

        String clazzName = clazz.getSimpleName();
        Set<String> packageNames = new HashSet<>();
        StringBuilder fieldText = new StringBuilder();
        String idTypeName = "";


        for (Field declaredField : clazz.getDeclaredFields()) {

//            fieldText.append("\tprivate final ");

            //Id field type controller
            if( "id".equals(declaredField.getName()) )
                idTypeName = declaredField.getType().getSimpleName();

            // collection or any object type -> Set, LIst, Integer, String, User
            if( declaredField.getType().getName().contains(".domain.") )
                fieldText.append(declaredField.getType().getSimpleName()+"Dto");
            else
                fieldText.append(declaredField.getType().getSimpleName());

            if( !declaredField.getType().getName().contains("java.lang") && !declaredField.getType().getName().contains(".domain.") )
                packageNames.add("import "+declaredField.getType().getName()+";");

            // Collection control
            if (Collection.class.isAssignableFrom(declaredField.getType())) {

                ParameterizedType stringListType = (ParameterizedType) declaredField.getGenericType();
                Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];

                if( stringListClass.getTypeName().contains(".domain.") )
                    fieldText.append("<"+stringListClass.getSimpleName()+"Dto>");
                else
                    fieldText.append("<"+stringListClass.getSimpleName()+">");

                if( !stringListClass.getTypeName().contains("java.lang") && !stringListClass.getTypeName().contains(".domain.") )
                    packageNames.add("import "+ stringListClass.getTypeName() +";");

            }

            // add field name
            fieldText.append(" "+declaredField.getName()+";\n");
        }

        // TODO put multithreading
        Controller.createControllerClass( clazzName );
        Mapper.createMapperClass( clazzName );
        Model.createModelClass( clazzName,  packageNames, fieldText.toString() );
        Service.createServiceClass( clazzName );
        Repository.createRepositoryClass( clazzName,  idTypeName );
    }

    // Create directory and files common function
    public static void createFileAndDirectory( String fileName, String text, String newPackageName ) {

        String path = System.getProperty("user.dir") + "/src/main/java/" + PACKAGE_NAME.replace(".","/") + "/" + newPackageName + "/" + fileName + ".java";

        File f = new File(path);
        if(!f.getParentFile().exists()){
            f.getParentFile().mkdirs();
        }
        //Remove if clause if you want to overwrite file
        if(!f.exists()){
            try {
                f.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            //dir will change directory and specifies file name for writer
            File dir = new File(f.getParentFile(), f.getName());
            PrintWriter writer = new PrintWriter(dir);
            writer.print(text);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    // Make first letter lower case UserModel -> userModel
    private static String makeFirstLetterLowerCase(String str){
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private static class Controller {

        public static void createControllerClass( String entityName ) {
            String templateStr = getControllerTemplate( entityName );
            createFileAndDirectory( entityName+"Controller", templateStr, "controller" );
        }

        private static String getControllerTemplate( String entityName ){

            StringBuilder template = new StringBuilder();

            String lowerCaseEntityName = makeFirstLetterLowerCase( entityName );

            template.append(
                    "package "+ PACKAGE_NAME +".controller;\n"+
                    "\n"+
                    "import "+ PACKAGE_NAME +".model."+ entityName +"Dto;\n"+
                    "import "+ PACKAGE_NAME +".service."+ entityName +"Service;\n"+
                    "import org.springframework.beans.factory.annotation.Autowired;\n"+
                    "import org.springframework.http.MediaType;\n"+
                    "import org.springframework.lang.NonNull;\n"+
                    "import org.springframework.web.bind.annotation.*;\n"+
                    "import reactor.core.publisher.Flux;\n"+
                    "import reactor.core.publisher.Mono;\n"+
                    "\n"+
                    "@RestController\n" +
                    "@RequestMapping(\""+ lowerCaseEntityName +"\")\n"+
                    "public class "+ entityName +"Controller {\n" +
                    "\n"+
                    "    @Autowired\n" +
                    "    private "+ entityName +"Service "+ lowerCaseEntityName +"Services;\n" +
                    "\n" +
                    "    @GetMapping(\"{id}\")\n" +
                    "    public Mono<"+ entityName +"Dto> getById( @PathVariable String id ) {\n" +
                    "        return "+ lowerCaseEntityName +"Services.getById( id );\n" +
                    "    }\n" +
                    "\n" +
                    "    @GetMapping(\"getAll\")\n" +
                    "    public Flux<"+ entityName +"Dto> getAll(){\n" +
                    "\n" +
                    "        return "+ lowerCaseEntityName +"Services.getAll();\n" +
                    "    }\n" +
                    "\n" +
                    "    @PostMapping( consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE } )\n" +
                    "    public Mono<"+ entityName +"Dto> create"+ entityName +"(@RequestBody @NonNull "+ entityName +"Dto "+ lowerCaseEntityName +"Dto) {\n" +
                    "        return "+ lowerCaseEntityName +"Services.create( "+ lowerCaseEntityName +"Dto );\n" +
                    "    }\n" +
                    "\n" +
                    "    @PutMapping( consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE } )\n" +
                    "    public Mono<"+entityName+"Dto> update(@RequestBody @NonNull "+entityName+"Dto "+ lowerCaseEntityName +"Dto) {\n" +
                    "        return "+ lowerCaseEntityName +"Services.update( "+ lowerCaseEntityName +"Dto );\n" +
                    "    }\n" +
                    "\n" +
                    "    @DeleteMapping(\"{id}\")\n" +
                    "    public Mono<Void> deleteById( @PathVariable String id ) {\n" +
                    "        return "+ lowerCaseEntityName +"Services.deleteById( id );\n" +
                    "    }\n" +
                    "\n" +
                    "}");

            return template.toString();

        }

    }

    private static class Mapper {

        public static void createMapperClass(String entityName) {
            String templateStr = getServiceTemplate(entityName);
            createFileAndDirectory(entityName + "Mapper", templateStr, "mapper");
        }

        private static String getServiceTemplate(String entityName) {

            StringBuilder template = new StringBuilder();
            String lowerCaseEntityName = makeFirstLetterLowerCase(entityName);

            template.append(
                    "package " + PACKAGE_NAME + ".mapper;\n" +
                    "\n" +
                    "import " + PACKAGE_NAME + ".domain." + entityName + ";\n" +
                    "import " + PACKAGE_NAME + ".model." + entityName + "Dto;\n" +
                    "import org.mapstruct.Mapper;\n" +
                    "import org.mapstruct.factory.Mappers;\n" +
                    "\n" +
                    "@Mapper\n" +
                    "public interface " + entityName + "Mapper {\n" +
                    "\n" +
                    "    " + entityName + "Mapper INSTANCE = Mappers.getMapper( " + entityName + "Mapper.class );\n" +
                    "\n" +
                    "    " + entityName + "Dto " + lowerCaseEntityName + "To" + entityName + "Dto( " + entityName + " " + lowerCaseEntityName + " );\n" +
                    "    " + entityName + " " + lowerCaseEntityName + "DtoTo" + entityName + "( " + entityName + "Dto " + lowerCaseEntityName + "Dto );\n" +
                    "\n" +
                    "}"
            );

            return template.toString();

        }
    }

    private static class Model {

        public static void createModelClass( String className, Set<String> packages, String fields ) {
            String templateStr = getModelTemplate( className, packages, fields );
            createFileAndDirectory( className+"Dto", templateStr, "model" );
        }

        private static String getModelTemplate( String entityName, Set<String> packages, String fields ){

            StringBuilder template = new StringBuilder();
            String lowerCaseClassName = makeFirstLetterLowerCase( entityName );

            template.append(
                    "package "+ PACKAGE_NAME +".model;\n" +
                            "\n");

            // add packages
            packages.forEach( p -> template.append( p+"\n" ) );

            template.append(
                    "import lombok.AllArgsConstructor;\n" +
                    "import lombok.Builder;\n" +
                    "import lombok.Value;\n" +
                    "\n" +
                    "@Value\n" +
                    "@Builder\n" +
                    "@AllArgsConstructor\n" +
                    "public class "+ entityName +"Dto {\n" +
                    "\n" );

                        // add fields
                        template.append( fields );

                        template.append(
                        "\n" +
                    "}"
            );

            return template.toString();

        }

    }

    private static class Repository {

        public static void createRepositoryClass( String entityName, String idFieldTypeName ) {
            String templateStr = getServiceTemplate( entityName, idFieldTypeName );
            createFileAndDirectory( entityName+"Repository", templateStr, "repository" );
        }

        private static String getServiceTemplate( String entityName, String idFieldTypeName ){

            StringBuilder template = new StringBuilder();

            template.append(
                    "package "+ PACKAGE_NAME +".repository;\n" +
                            "\n" +
                            "import "+ PACKAGE_NAME +".domain."+entityName+";\n" +
                            "import org.springframework.data.mongodb.repository.ReactiveMongoRepository;\n" +
                            "import org.springframework.stereotype.Repository;\n" +
                            "\n" +
                            "@Repository\n" +
                            "public interface "+entityName+"Repository extends ReactiveMongoRepository<"+ entityName +", "+ idFieldTypeName +"> {\n" +
                            "\n" +
                            "}"
            );

            return template.toString();

        }

    }

    private static class Service {

        public static void createServiceClass( String entityName ) {
            String serviceTemplateStr = getServiceTemplate( entityName );
            createFileAndDirectory( entityName+"Service", serviceTemplateStr, "service" );

            String serviceImplTemplateStr = getServiceImplTemplate( entityName );
            createFileAndDirectory( entityName+"ServiceImpl", serviceImplTemplateStr, "service" );
        }

        private static String getServiceTemplate(String entityName){
            StringBuilder template = new StringBuilder();

            String lowerCaseEntityName = makeFirstLetterLowerCase(entityName);

            template.append(
                    "package "+ PACKAGE_NAME +".service;\n" +
                    "\n" +
                    "import "+ PACKAGE_NAME +".model."+ entityName +"Dto;\n" +
                    "import reactor.core.publisher.Flux;\n" +
                    "import reactor.core.publisher.Mono;\n" +
                    "\n" +
                    "public interface "+ entityName +"Service {\n" +
                    "\n" +
                    "    Mono<"+ entityName +"Dto> create( "+ entityName +"Dto "+lowerCaseEntityName+"Dto );\n" +
                    "    Mono<"+ entityName +"Dto> update( "+ entityName +"Dto "+lowerCaseEntityName+"Dto );\n" +
                    "    Mono<Void> deleteById( String id );\n" +
                    "    Mono<"+ entityName +"Dto> getById( String id );\n" +
                    "    Flux<"+ entityName +"Dto> getAll();\n" +
                    "\n" +
                    "}");

            return template.toString();

        }


        private static String getServiceImplTemplate(String entityName){
            StringBuilder template = new StringBuilder();

            String lowerCaseEntityName = makeFirstLetterLowerCase(entityName);

            // userMapper
            String mapperFieldName = lowerCaseEntityName +"Mapper";

            template.append(
                    "package "+PACKAGE_NAME+".service;\n" +
                    "\n" +
                    "import "+PACKAGE_NAME+".mapper."+entityName+"Mapper;\n" +
                    "import "+PACKAGE_NAME+".model."+entityName+"Dto;\n" +
                    "import "+PACKAGE_NAME+".repository."+entityName+"Repository;\n" +
                    "import org.springframework.beans.factory.annotation.Autowired;\n" +
                    "import org.springframework.stereotype.Service;\n" +
                    "import reactor.core.publisher.Flux;\n" +
                    "import reactor.core.publisher.Mono;\n" +
                    "\n" +
                    "@Service\n" +
                    "public class "+ entityName +"ServiceImpl implements "+ entityName +"Service {\n" +
                    "\n" +
                    "    private "+ entityName +"Mapper "+ mapperFieldName +" = "+ entityName +"Mapper.INSTANCE;\n" +
                    "\n" +
                    "    @Autowired\n" +
                    "    private "+ entityName +"Repository "+ lowerCaseEntityName +"Repository;\n" +
                    "\n" +
                    "    @Override\n" +
                    "    public Mono<"+ entityName +"Dto> create( "+ entityName +"Dto "+ lowerCaseEntityName +"Dto) {\n" +
                    "\n" +
                    "        return "+ lowerCaseEntityName +"Repository.insert( "+ mapperFieldName +"."+ lowerCaseEntityName +"DtoTo"+ entityName +"( "+ lowerCaseEntityName +"Dto ) )\n" +
                    "                .map( "+ mapperFieldName +"::"+ lowerCaseEntityName +"To"+ entityName +"Dto );\n" +
                    "    }\n" +
                    "\n" +
                    "    @Override\n" +
                    "    public Mono<"+ entityName +"Dto> update("+ entityName +"Dto "+ lowerCaseEntityName +"Dto) {\n" +
                    "\n" +
                    "        return "+ lowerCaseEntityName +"Repository.save( "+ mapperFieldName +"."+ lowerCaseEntityName +"DtoTo"+ entityName +"( "+ lowerCaseEntityName +"Dto ))\n" +
                    "                .map( "+ mapperFieldName +"::"+ lowerCaseEntityName +"To"+ entityName +"Dto );\n" +
                    "    }\n" +
                    "\n" +
                    "    @Override\n" +
                    "    public Mono<Void> deleteById(String id) {\n" +
                    "\n" +
                    "        return "+ lowerCaseEntityName +"Repository.deleteById(id);\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "    @Override\n" +
                    "    public Mono<"+ entityName +"Dto> getById(String id) {\n" +
                    "\n" +
                    "        return "+ lowerCaseEntityName +"Repository.findById(id)\n" +
                    "                .map( "+ mapperFieldName +"::"+ lowerCaseEntityName +"To"+ entityName +"Dto );\n" +
                    "    }\n" +
                    "\n" +
                    "    @Override\n" +
                    "    public Flux<"+ entityName +"Dto> getAll() {\n" +
                    "\n" +
                    "        return "+ lowerCaseEntityName +"Repository.findAll()\n" +
                    "                .map( "+ mapperFieldName +"::"+ lowerCaseEntityName +"To"+ entityName +"Dto );\n" +
                    "    }\n" +
                    "\n" +
                    "}"
            );

            return template.toString();

        }

    }

}
