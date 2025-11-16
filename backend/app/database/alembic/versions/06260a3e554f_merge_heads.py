"""merge heads

Revision ID: 06260a3e554f
Revises: 07ef0ed961d1, ae3935ab297a
Create Date: 2025-11-16 03:28:47.417412

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '06260a3e554f'
down_revision: Union[str, Sequence[str], None] = ('07ef0ed961d1', 'ae3935ab297a')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
