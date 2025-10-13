# This is necessary to ensure that all models are registered with SQLAlchemy's metadata.
import app.features.auth.models  # noqa: F401
import app.features.journal.models  # noqa: F401
import app.features.user.models  # noqa: F401
