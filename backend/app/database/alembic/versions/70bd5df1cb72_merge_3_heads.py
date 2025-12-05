"""merge 3 heads

Revision ID: 70bd5df1cb72
Revises: 291418e89a93, ae3935ab297a, c8bf0a75ed3a
Create Date: 2025-11-24 04:17:03.359551

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '70bd5df1cb72'
down_revision: Union[str, Sequence[str], None] = ('291418e89a93', 'ae3935ab297a', 'c8bf0a75ed3a')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
