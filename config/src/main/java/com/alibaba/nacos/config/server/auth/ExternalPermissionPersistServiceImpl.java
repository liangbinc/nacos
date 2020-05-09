/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.config.server.auth;

import com.alibaba.nacos.config.server.configuration.ConditionOnEmbeddedStorage;
import com.alibaba.nacos.config.server.configuration.ConditionOnExternalStorage;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.config.server.service.repository.ExternalStoragePersistServiceImpl;
import com.alibaba.nacos.config.server.service.repository.PaginationHelper;
import com.alibaba.nacos.config.server.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

import static com.alibaba.nacos.config.server.service.RowMapperManager.PERMISSION_ROW_MAPPER;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@Conditional(value = ConditionOnExternalStorage.class)
@Component
public class ExternalPermissionPersistServiceImpl implements PermissionPersistService {

	@Autowired
	private ExternalStoragePersistServiceImpl persistService;

	private JdbcTemplate jt;

	@PostConstruct
	protected void init() {
		jt = persistService.getJdbcTemplate();
	}

	public Page<PermissionInfo> getPermissions(String role, int pageNo, int pageSize) {
		PaginationHelper<PermissionInfo> helper = persistService.createPaginationHelper();

		String sqlCountRows = "select count(*) from permissions where ";
		String sqlFetchRows
				= "select role,resource,action from permissions where ";

		String where = " role='" + role + "' ";

		if (StringUtils.isBlank(role)) {
			where = " 1=1 ";
		}

		try {
			Page<PermissionInfo> pageInfo = helper.fetchPage(sqlCountRows
							+ where, sqlFetchRows + where, new ArrayList<String>().toArray(), pageNo,
					pageSize, PERMISSION_ROW_MAPPER);

			if (pageInfo==null) {
				pageInfo = new Page<>();
				pageInfo.setTotalCount(0);
				pageInfo.setPageItems(new ArrayList<>());
			}

			return pageInfo;

		} catch (CannotGetJdbcConnectionException e) {
			LogUtil.fatalLog.error("[db-error] " + e.toString(), e);
			throw e;
		}
	}

	public void addPermission(String role, String resource, String action) {

		String sql = "INSERT into permissions (role, resource, action) VALUES (?, ?, ?)";

		try {
			jt.update(sql, role, resource, action);
		} catch (CannotGetJdbcConnectionException e) {
			LogUtil.fatalLog.error("[db-error] " + e.toString(), e);
			throw e;
		}
	}

	public void deletePermission(String role, String resource, String action) {

		String sql = "DELETE from permissions WHERE role=? and resource=? and action=?";
		try {
			jt.update(sql, role, resource, action);
		} catch (CannotGetJdbcConnectionException e) {
			LogUtil.fatalLog.error("[db-error] " + e.toString(), e);
			throw e;
		}
	}

}