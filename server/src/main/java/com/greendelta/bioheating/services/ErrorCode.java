package com.greendelta.bioheating.services;

/// Error codes that are returned to the client. The client translates these
/// codes into messages of the user's language (see `errors` in the UI
/// translation files). When adding a code here, also add its translations
/// there.
public enum ErrorCode {

	// general
	NOT_AUTHENTICATED,
	ACCESS_DENIED,
	ADMIN_REQUIRED,
	TASK_NOT_FOUND,
	TASK_FAILED,

	// users
	USER_NOT_FOUND,
	INVALID_USER_DATA,
	USER_NAME_EXISTS,
	LAST_ADMIN,
	ADMIN_NOT_DELETABLE,
	UNSUPPORTED_LANGUAGE,

	// projects
	PROJECT_NOT_FOUND,
	PROJECT_LOAD_FAILED,
	PROJECT_SAVE_FAILED,
	PROJECT_DELETE_FAILED,
	PROJECT_IMPORT_FAILED,
	PROJECT_UPDATE_FAILED,
	NO_UPDATE_SELECTED,
	UPLOAD_FAILED,
	NO_VALID_FILES,

	// building estimation
	INVALID_BUILDING_DATA,
	NO_CLIMATE_REGION,
	BUILDING_NOT_HEATED,
	ESTIMATION_FAILED,

	// solutions & exports
	SOLUTION_NOT_FOUND,
	SOLUTION_IMAGE_NOT_FOUND,
	CALCULATION_FAILED,
	EXPORT_FAILED;

	/// Creates a failed result with this error code and technical details.
	public <T> Coded<T> fail(String details) {
		return Coded.error(this, details);
	}

	/// The JSON body of an error response: a code that the client translates
	/// and optional technical details (English) for logging and debugging.
	public record ApiError(ErrorCode code, String details) {
	}

	/// A result that carries an error code in case of an error.
	public record Coded<T>(T value, ErrorCode error, String details) {

		public static <T> Coded<T> ok(T value) {
			return new Coded<>(value, null, null);
		}

		public static <T> Coded<T> error(ErrorCode code, String details) {
			return new Coded<>(null, code, details);
		}

		public boolean isError() {
			return error != null;
		}

		public ApiError apiError() {
			return new ApiError(error, details);
		}
	}
}
