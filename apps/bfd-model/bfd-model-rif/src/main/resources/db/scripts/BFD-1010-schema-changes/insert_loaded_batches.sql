insert into public.loaded_batches (
	loaded_batchid,
	loaded_fileid,
	beneficiaries,
	created
)
select
	"loadedBatchId",
	"loadedFileId",
	"beneficiaries",
	"created"
from
	public."LoadedBatches"
on conflict on constraint
	loaded_batches_pkey
do nothing;

insert into public.loaded_files (
    loaded_fileid,
    rif_type,
    created
)
select
	"loadedFileId",
	"rifType",
	"created"
from
	public."LoadedFiles"
on conflict on constraint
	loaded_files_pkey
do nothing;