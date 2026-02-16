package services;

import entity.EvaluationPerformance;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EvaluationPerformanceService {

    public void add(EvaluationPerformance e) throws SQLException {
        String sql = """
            INSERT INTO evaluation_performance (id_affectation, note, qualite, commentaire, date_evaluation)
            VALUES (?, ?, ?, ?, ?)
            """;
        Connection cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, e.getIdAffectation());
            ps.setInt(2, e.getNote());
            ps.setString(3, e.getQualite());
            ps.setString(4, e.getCommentaire());
            ps.setDate(5, e.getDateEvaluation() != null ? Date.valueOf(e.getDateEvaluation()) : null);
            ps.executeUpdate();
        }
    }

    public void update(EvaluationPerformance e) throws SQLException {
        String sql = """
            UPDATE evaluation_performance
            SET id_affectation = ?, note = ?, qualite = ?, commentaire = ?, date_evaluation = ?
            WHERE id_evaluation = ?
            """;
        Connection cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, e.getIdAffectation());
            ps.setInt(2, e.getNote());
            ps.setString(3, e.getQualite());
            ps.setString(4, e.getCommentaire());
            ps.setDate(5, e.getDateEvaluation() != null ? Date.valueOf(e.getDateEvaluation()) : null);
            ps.setInt(6, e.getIdEvaluation());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM evaluation_performance WHERE id_evaluation = ?";
        Connection cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<EvaluationPerformance> getAll() throws SQLException {
        String sql = "SELECT id_evaluation, id_affectation, note, qualite, commentaire, date_evaluation FROM evaluation_performance";
        List<EvaluationPerformance> list = new ArrayList<>();
        Connection cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public EvaluationPerformance getById(int id) throws SQLException {
        String sql = "SELECT id_evaluation, id_affectation, note, qualite, commentaire, date_evaluation FROM evaluation_performance WHERE id_evaluation = ?";
        Connection cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<EvaluationPerformance> getByAffectation(int idAffectation) throws SQLException {
        String sql = "SELECT id_evaluation, id_affectation, note, qualite, commentaire, date_evaluation FROM evaluation_performance WHERE id_affectation = ?";
        List<EvaluationPerformance> list = new ArrayList<>();
        Connection cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAffectation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public double averageNoteByAffectation(int idAffectation) throws SQLException {
        String sql = "SELECT AVG(note) FROM evaluation_performance WHERE id_affectation = ?";
        Connection cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) throw new SQLException("No connection");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAffectation);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? 0.0 : avg;
                }
            }
        }
        return 0.0;
    }

    private EvaluationPerformance mapResultSet(ResultSet rs) throws SQLException {
        EvaluationPerformance e = new EvaluationPerformance();
        e.setIdEvaluation(rs.getInt("id_evaluation"));
        e.setIdAffectation(rs.getInt("id_affectation"));
        e.setNote(rs.getInt("note"));
        e.setQualite(rs.getString("qualite"));
        e.setCommentaire(rs.getString("commentaire"));
        Date d = rs.getDate("date_evaluation");
        e.setDateEvaluation(d != null ? d.toLocalDate() : null);
        return e;
    }
}
