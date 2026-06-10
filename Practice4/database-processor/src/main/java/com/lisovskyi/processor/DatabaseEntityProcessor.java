package com.lisovskyi.processor;

import com.google.auto.service.AutoService;
import com.lisovskyi.annotations.DatabaseEntity;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.lisovskyi.annotations.DatabaseEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class DatabaseEntityProcessor extends AbstractProcessor {

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || annotations.isEmpty()) {
            return false;
        }

        Set<? extends Element> databaseAnnotations = roundEnv.getElementsAnnotatedWith(DatabaseEntity.class);

        for (Element element : databaseAnnotations) {
            String className = element.getSimpleName().toString();
            PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);

            String originalPackageName = packageElement.getQualifiedName().toString();
            String repositoryPackageName = originalPackageName.replace(
                    originalPackageName.substring(originalPackageName.lastIndexOf(".")),
                    ".repository"
            );
            String validatorPackageName = originalPackageName.replace(
                    originalPackageName.substring(originalPackageName.lastIndexOf(".")),
                    ".validator"
            );

            List<? extends Element> fields = element.getEnclosedElements()
                    .stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .toList();

            String tableName = resolveTableName(className);
            String insertMethod = buildInsertMethod(className, tableName, fields);
            String findByIdMethod = buildFindByIdMethod(className, tableName, fields);

            String repositoryContent = buildRepositoryClassContent(
                    repositoryPackageName,
                    originalPackageName,
                    className,
                    insertMethod,
                    findByIdMethod
            );
            writeSourceFile(repositoryPackageName, className + "Repository", repositoryContent);

            String validatorContent = buildValidatorClass(
                    validatorPackageName,
                    originalPackageName,
                    className,
                    fields
            );
            writeSourceFile(validatorPackageName, className + "Validator", validatorContent);
        }

        return true;
    }

    private void writeSourceFile(final String packageName, final String simpleClassName, final String content) {
        try {
            JavaFileObject file = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + simpleClassName);

            try (Writer writer = file.openWriter()) {
                writer.write(content);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printError(
                    "Cannot create file " + simpleClassName + ": " + e.getMessage()
            );
        }
    }

    private String buildRepositoryClassContent(
            final String repositoryPackageName,
            final String originalPackageName,
            final String className,
            final String insertMethod,
            final String findByIdMethod
    ) {
        return """
                package %s;

                import %s.%s;
                import javax.sql.DataSource;
                import java.sql.Connection;
                import java.sql.PreparedStatement;
                import java.sql.SQLException;

                public class %sRepository {

                    private final DataSource dataSource;

                    public %sRepository(final DataSource dataSource) {
                        this.dataSource = dataSource;
                    }

                %s

                %s
                }
                """.formatted(
                repositoryPackageName,
                originalPackageName,
                className,
                className,
                className,
                insertMethod,
                findByIdMethod
        );
    }

    private String buildInsertMethod(
            final String className,
            final String tableName,
            final List<? extends Element> fields
    ) {
        String sqlQuery = buildInsertSqlQuery(tableName, fields);
        String jdbcSetters = buildJdbcSetters(fields);

        return """
                    public void insert(final %s entity) {
                        String sql = "%s";
                        try (Connection connection = dataSource.getConnection();
                                PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                            System.out.println("Executing SQL query: " + sql);
                %s
                            int rowsAffected = stmt.executeUpdate();
                            if (rowsAffected == 0) {
                                throw new SQLException("Error while inserting into database");
                            }

                            System.out.println("Entity is saved to database");
                        } catch (SQLException e) {
                            throw new RuntimeException("Database error: " + e.getMessage(), e);
                        }
                    }
                """.formatted(className, sqlQuery, jdbcSetters);
    }

    private String buildFindByIdMethod(
            final String className,
            final String tableName,
            final List<? extends Element> fields
    ) {
        StringBuilder setters = new StringBuilder();

        for (Element field : fields) {
            String fieldType = field.asType().toString();
            String fieldName = field.getSimpleName().toString();
            String columnName = getColumnName(field);
            String setterName = "set" + capitalize(fieldName);
            String resultSetGetter = getJdbcGetterName(fieldType);

            setters.append(
                    "                            entity.%s(rs.%s(\"%s\"));%n"
                            .formatted(setterName, resultSetGetter, columnName)
            );
        }

        return """
                    public %s findById(final Long id) {
                        String sql = "SELECT * FROM %s WHERE id = ?";
                        try (Connection connection = dataSource.getConnection();
                            PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                            stmt.setLong(1, id);

                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    %s entity = new %s();
                %s
                                    return entity;
                                }
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException("Database error during findById: " + e.getMessage(), e);
                        }

                        return null;
                    }
                """.formatted(
                className,
                tableName,
                className,
                className,
                setters.toString().stripTrailing()
        );
    }

    private String buildValidatorClass(
            final String validatorPackageName,
            final String originalPackageName,
            final String className,
            final List<? extends Element> fields
    ) {
        String checks = buildValidationChecks(fields);

        return """
                package %s;

                import com.lisovskyi.annotations.RuntimeValidate;
                import %s.%s;

                public class %sValidator {

                    @RuntimeValidate
                    public void validate(final %s entity) {
                %s
                    }
                }
                """.formatted(
                validatorPackageName,
                originalPackageName,
                className,
                className,
                className,
                checks.isEmpty() ? "        // no runtime validations defined" : checks
        );
    }

    private String buildValidationChecks(final List<? extends Element> fields) {
        StringBuilder sb = new StringBuilder();

        for (Element field : fields) {
            boolean hasRuntimeValidate = field.getAnnotationMirrors().stream()
                    .anyMatch(annotationMirror ->
                            annotationMirror.getAnnotationType().toString()
                                    .equals("com.lisovskyi.annotations.RuntimeValidate")
                    );

            if (hasRuntimeValidate) {
                String fieldName = field.getSimpleName().toString();
                String getterName = "get" + capitalize(fieldName);

                sb.append("""
                                if (entity.%s() == null) {
                                    throw new RuntimeException("Validation failed: field '%s' cannot be null");
                                }
                        """.formatted(getterName, fieldName));
            }
        }

        return sb.toString();
    }

    private String buildJdbcSetters(final List<? extends Element> fields) {
        StringBuilder sb = new StringBuilder();
        int index = 1;

        for (Element field : fields) {
            String jdbcSetterName = getJdbcSetterName(field.asType().toString());
            String fieldName = field.getSimpleName().toString();
            String getterName = "get" + capitalize(fieldName) + "()";

            sb.append("                            stmt.%s(%d, entity.%s);%n"
                    .formatted(jdbcSetterName, index, getterName));
            index++;
        }

        return sb.toString().stripTrailing();
    }

    private String buildInsertSqlQuery(final String tableName, final List<? extends Element> fields) {
        String columns = fields.stream()
                .map(this::getColumnName)
                .collect(Collectors.joining(", "));

        String placeholders = fields.stream()
                .map(_ -> "?")
                .collect(Collectors.joining(", "));

        return "INSERT INTO %s (%s) VALUES (%s);".formatted(tableName, columns, placeholders);
    }

    private String getColumnName(final Element field) {
        return field.getAnnotationMirrors().stream()
                .filter(annotationMirror ->
                        annotationMirror.getAnnotationType().toString()
                                .equals("com.lisovskyi.annotations.ColumnMapping")
                )
                .findFirst()
                .flatMap(annotationMirror -> annotationMirror.getElementValues().entrySet().stream()
                        .filter(entry -> entry.getKey().getSimpleName().contentEquals("value"))
                        .map(entry -> entry.getValue().getValue().toString())
                        .filter(value -> !value.isBlank())
                        .findFirst())
                .orElse(toSnakeCase(field.getSimpleName().toString()));
    }

    private String resolveTableName(final String className) {
        return pluralize(toSnakeCase(className));
    }

    private String toSnakeCase(final String value) {
        return value
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }

    private String pluralize(final String value) {
        if (value.endsWith("y") && value.length() > 1 && !isVowel(value.charAt(value.length() - 2))) {
            return value.substring(0, value.length() - 1) + "ies";
        }

        if (value.endsWith("s") || value.endsWith("x") || value.endsWith("z")
                || value.endsWith("ch") || value.endsWith("sh")) {
            return value + "es";
        }

        return value + "s";
    }

    private boolean isVowel(final char ch) {
        return "aeiou".indexOf(Character.toLowerCase(ch)) >= 0;
    }

    private String capitalize(final String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private enum TypeName {
        SETTER,
        GETTER
    }

    private String getJdbcName(final String fieldType, final TypeName typeName) {
        char prefix = switch (typeName) {
            case GETTER -> 'g';
            case SETTER -> 's';
        };

        return switch (fieldType) {
            case "java.lang.String" -> prefix + "etString";
            case "int", "java.lang.Integer" -> prefix + "etInt";
            case "long", "java.lang.Long" -> prefix + "etLong";
            case "double", "java.lang.Double" -> prefix + "etDouble";
            case "boolean", "java.lang.Boolean" -> prefix + "etBoolean";
            default -> prefix + "etObject";
        };
    }

    private String getJdbcSetterName(final String fieldType) {
        return getJdbcName(fieldType, TypeName.SETTER);
    }

    private String getJdbcGetterName(final String fieldType) {
        return getJdbcName(fieldType, TypeName.GETTER);
    }
}
