// Copyright 2018 The casbin Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.casbin.adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.casbin.jcasbin.model.Assertion;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.Helper;

class CasbinRule {
    String ptype;
    String v0;
    String v1;
    String v2;
    String v3;
    String v4;
    String v5;
}

/**
 * JDBCAdapter is the JDBC adapter for jCasbin.
 * It can load policy from JDBC supported database or save policy to it.
 */
public class JDBCAdapter implements Adapter {
	
	private DataSource dataSource = null;
	private String tableName = "casbin_rule";
	
    /**
     * JDBCAdapter is the constructor for JDBCAdapter.
     *
     * @param driver   the JDBC driver, like "com.mysql.cj.jdbc.Driver".
     * @param url      the JDBC URL, like "jdbc:mysql://localhost:3306/casbin".
     * @param username the username of the database.
     * @param password the password of the database.
     */
	public JDBCAdapter(String driver, String url, String username, String password) throws Exception {
		this(new JDBCDataSource(driver, url, username, password));
	}
    
    /**
     * The constructor for JDBCAdapter, will not try to create database.
     *
     * @param dataSource the JDBC DataSource.
     */
    public JDBCAdapter(DataSource dataSource) throws Exception {
        this.dataSource = dataSource;
        migrate();
	}
    
    /**
     * The constructor for JDBCAdapter, will not try to create database.
     *
     * @param dataSource the JDBC DataSource.
     * @param tableName the database tableName is casbin_rule by default.
     */
    public JDBCAdapter(DataSource dataSource, String tableName) throws Exception {
        this.dataSource = dataSource;
        this.tableName = tableName;
        migrate();
    }
    
	private void migrate() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + "(ptype VARCHAR(100) NOT NULL, v0 VARCHAR(100), v1 VARCHAR(100), v2 VARCHAR(100), v3 VARCHAR(100), v4 VARCHAR(100), v5 VARCHAR(100))";
			String productName = conn.getMetaData().getDatabaseProductName();

			switch (productName) {
			case "Oracle":
				sql = "declare begin execute immediate 'CREATE TABLE CASBIN_RULE(ptype VARCHAR(100) not NULL, v0 VARCHAR(100), v1 VARCHAR(100), v2 VARCHAR(100), v3 VARCHAR(100), v4 VARCHAR(100), v5 VARCHAR(100))'; exception when others then if SQLCODE = -955 then null; else raise; end if; end;";
				break;
			case "Microsoft SQL Server":
				sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='" + tableName + "' and xtype='U') CREATE TABLE " + tableName + "(ptype VARCHAR(100) NOT NULL, v0 VARCHAR(100), v1 VARCHAR(100), v2 VARCHAR(100), v3 VARCHAR(100), v4 VARCHAR(100), v5 VARCHAR(100))";
				break;
            }
            
            stmt.executeUpdate(sql);
        }
    }

    private void loadPolicyLine(CasbinRule line, Model model) {
        String lineText = line.ptype;
        if (!line.v0.equals("")) {
            lineText += ", " + line.v0;
        }
        if (!line.v1.equals("")) {
            lineText += ", " + line.v1;
        }
        if (!line.v2.equals("")) {
            lineText += ", " + line.v2;
        }
        if (!line.v3.equals("")) {
            lineText += ", " + line.v3;
        }
        if (!line.v4.equals("")) {
            lineText += ", " + line.v4;
        }
        if (!line.v5.equals("")) {
            lineText += ", " + line.v5;
        }

        Helper.loadPolicyLine(lineText, model);
    }

    /**
     * loadPolicy loads all policy rules from the storage.
     */
    @Override
    public void loadPolicy(Model model) {
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rSet = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData rData = rSet.getMetaData();
            while (rSet.next()) {
                CasbinRule line = new CasbinRule();
                for (int i = 1; i <= rData.getColumnCount(); i++) {
                    if (i == 1) {
                        line.ptype = rSet.getObject(i) == null ? "" : (String) rSet.getObject(i);
                    } else if (i == 2) {
                        line.v0 = rSet.getObject(i) == null ? "" : (String) rSet.getObject(i);
                    } else if (i == 3) {
                        line.v1 = rSet.getObject(i) == null ? "" : (String) rSet.getObject(i);
                    } else if (i == 4) {
                        line.v2 = rSet.getObject(i) == null ? "" : (String) rSet.getObject(i);
                    } else if (i == 5) {
                        line.v3 = rSet.getObject(i) == null ? "" : (String) rSet.getObject(i);
                    } else if (i == 6) {
                        line.v4 = rSet.getObject(i) == null ? "" : (String) rSet.getObject(i);
                    } else if (i == 7) {
                        line.v5 = rSet.getObject(i) == null ? "" : (String) rSet.getObject(i);
                    }
                }
                loadPolicyLine(line, model);
            }
            rSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    private CasbinRule savePolicyLine(String ptype, List<String> rule) {
        CasbinRule line = new CasbinRule();

        line.ptype = ptype;
        if (rule.size() > 0) {
            line.v0 = rule.get(0);
        }
        if (rule.size() > 1) {
            line.v1 = rule.get(1);
        }
        if (rule.size() > 2) {
            line.v2 = rule.get(2);
        }
        if (rule.size() > 3) {
            line.v3 = rule.get(3);
        }
        if (rule.size() > 4) {
            line.v4 = rule.get(4);
        }
        if (rule.size() > 5) {
            line.v5 = rule.get(5);
        }

        return line;
    }

    /**
     * savePolicy saves all policy rules to the storage.
     */
    @Override
    public void savePolicy(Model model) {
        String cleanSql = "delete from " + tableName;
        String addSql = "INSERT INTO " + tableName + " (ptype,v0,v1,v2,v3,v4,v5) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            final int batchSize = 1000;
            int count = 0;

            try (Statement statement = conn.createStatement(); PreparedStatement ps = conn.prepareStatement(addSql)) {
                statement.execute(cleanSql);

                for (Map.Entry<String, Assertion> entry : model.model.get("p").entrySet()) {
                    String ptype = entry.getKey();
                    Assertion ast = entry.getValue();

                    for (List<String> rule : ast.policy) {
                        CasbinRule line = savePolicyLine(ptype, rule);

                        ps.setString(1, line.ptype);
                        ps.setString(2, line.v0);
                        ps.setString(3, line.v1);
                        ps.setString(4, line.v2);
                        ps.setString(5, line.v3);
                        ps.setString(6, line.v4);
                        ps.setString(7, line.v5);

                        ps.addBatch();
                        if (++count % batchSize == 0) {
                            ps.executeBatch();
                        }
                    }
                }

                for (Map.Entry<String, Assertion> entry : model.model.get("g").entrySet()) {
                    String ptype = entry.getKey();
                    Assertion ast = entry.getValue();

                    for (List<String> rule : ast.policy) {
                        CasbinRule line = savePolicyLine(ptype, rule);

                        ps.setString(1, line.ptype);
                        ps.setString(2, line.v0);
                        ps.setString(3, line.v1);
                        ps.setString(4, line.v2);
                        ps.setString(5, line.v3);
                        ps.setString(6, line.v4);
                        ps.setString(7, line.v5);

                        ps.addBatch();
                        if (++count % batchSize == 0) {
                            ps.executeBatch();
                        }
                    }
                }

                ps.executeBatch();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();

                e.printStackTrace();
                throw new Error(e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    /**
     * addPolicy adds a policy rule to the storage.
     */
    @Override
    public void addPolicy(String sec, String ptype, List<String> rule) {
        if (CollectionUtils.isEmpty(rule)) return;

        String sql = "INSERT INTO " + tableName + " (ptype,v0,v1,v2,v3,v4,v5) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            CasbinRule line = savePolicyLine(ptype, rule);

            ps.setString(1, line.ptype);
            ps.setString(2, line.v0);
            ps.setString(3, line.v1);
            ps.setString(4, line.v2);
            ps.setString(5, line.v3);
            ps.setString(6, line.v4);
            ps.setString(7, line.v5);
            ps.addBatch();

            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    /**
     * removePolicy removes a policy rule from the storage.
     */
    @Override
    public void removePolicy(String sec, String ptype, List<String> rule) {
        if (CollectionUtils.isEmpty(rule)) return;
        removeFilteredPolicy(sec, ptype, 0, rule.toArray(new String[0]));
    }

    /**
     * removeFilteredPolicy removes policy rules that match the filter from the storage.
     */
    @Override
    public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
        List<String> values = Optional.of(Arrays.asList(fieldValues)).orElse(new ArrayList<>());
        if (CollectionUtils.isEmpty(values)) return;
        String sql = "DELETE FROM " + tableName + " WHERE ptype = ?";
        int columnIndex = fieldIndex;
        for (int i = 0; i < values.size(); i++) {
            sql = String.format("%s%s%s%s", sql, " AND v", columnIndex, " = ?");
            columnIndex++;
        }

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ptype);
            for (int j = 0; j < values.size(); j++) {
                ps.setString(j + 2, values.get(j));
            }

            ps.addBatch();

            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }
}
