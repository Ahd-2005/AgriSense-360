package services;

import java.sql.SQLException;
import java.util.List;


public interface IServiceStock<T> {
    int ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<T> afficher() throws SQLException;
    T recupererParId(int id) throws SQLException;
}
