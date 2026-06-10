package com.lisovskyi.orm;

import com.lisovskyi.annotations.RuntimeValidate;
import exception.OrmException;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class OrmManager {
    private final DataSource dataSource;
    private final Map<Class<?>, Object> repositoryRegistry = new HashMap<>();

    public OrmManager(final DataSource dataSource) {
        this.dataSource = dataSource;
        scanPackage("com.lisovskyi");
    }

    public void saveEntity(Object entity) {
        try {
            Class<?> entityClass = entity.getClass();

            runGeneratedValidator(entity, entityClass);

            Object repositoryInstance = repositoryRegistry.get(entityClass);
            if (repositoryInstance == null) {
                throw new OrmException("Repository was not found for class: " + entityClass.getName());
            }

            Method insertMethod = repositoryInstance.getClass().getMethod("insert", entityClass);
            insertMethod.invoke(repositoryInstance, entity);

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new OrmException("ORM Error: " + cause.getMessage(), cause);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T findById(Class<T> entityClass, Long id) {
        try {
            Object repositoryInstance = repositoryRegistry.get(entityClass);
            if (repositoryInstance == null) {
                throw new OrmException("Repository was not found for class: " + entityClass.getName());
            }

            Method findByIdMethod = repositoryInstance.getClass().getMethod("findById", Long.class);
            return (T) findByIdMethod.invoke(repositoryInstance, id);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new OrmException("Error executing findById: " + cause.getMessage(), cause);
        }
    }

    private void runGeneratedValidator(final Object entity, final Class<?> entityClass) {
        String pkg = entityClass.getPackageName();
        String basePkg = pkg.substring(0, pkg.lastIndexOf("."));
        String validatorClassName = basePkg + ".validator." + entityClass.getSimpleName() + "Validator";

        try {
            Class<?> validatorClass = Class.forName(validatorClassName);

            for (Method method : validatorClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RuntimeValidate.class)) {
                    Object validatorInstance = validatorClass.getDeclaredConstructor().newInstance();
                    method.invoke(validatorInstance, entity);
                }
            }
        } catch (ClassNotFoundException ignored) {

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new OrmException("Validation error: " + cause.getMessage(), cause);
        }
    }

    public void scanPackage(final String basePackage) {
        try {
            String pathToBasePackage = basePackage.replace(".", "/");
            var resource = Thread.currentThread().getContextClassLoader().getResource(pathToBasePackage);
            if (resource == null) return;

            File directory = new File(resource.getFile());
            File[] files = directory.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.isDirectory()) {
                    String subPackage = basePackage.isEmpty()
                            ? file.getName()
                            : basePackage + "." + file.getName();
                    scanPackage(subPackage);
                } else if (file.isFile() && file.getName().endsWith("Repository.class")) {
                    String repoFullName = basePackage + "."
                            + file.getName().substring(0, file.getName().lastIndexOf("."));
                    Class<?> repositoryClass = Class.forName(repoFullName);

                    Constructor<?> constructor = repositoryClass.getConstructor(DataSource.class);
                    Object repositoryInstance = constructor.newInstance(dataSource);

                    Class<?> entityClass = null;
                    for (Method method : repositoryClass.getMethods()) {
                        if (method.getName().equals("insert") && method.getParameterCount() == 1) {
                            entityClass = method.getParameterTypes()[0];
                            break;
                        }
                    }

                    if (entityClass != null) {
                        repositoryRegistry.put(entityClass, repositoryInstance);
                    }
                }
            }
        } catch (Exception e) {
            throw new OrmException("Failed to scan package or initialize repositories: " + e.getMessage(), e);
        }
    }
}
