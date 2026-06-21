package proj.mobapp.sudoku_sliding_puzzles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PuzzleAdapter extends RecyclerView.Adapter<PuzzleAdapter.TileViewHolder> {

    public interface OnTileClickListener {
        void onTileClick(int position);
    }

    private final List<PuzzleTile> tiles;
    private final OnTileClickListener listener;

    public PuzzleAdapter(List<PuzzleTile> tiles, OnTileClickListener listener) {
        this.tiles = tiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_puzzle, parent, false);
        return new TileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        PuzzleTile tile = tiles.get(position);

        if (tile.isBlank()) {
            holder.tileNumber.setText("");
            holder.tileNumber.setAlpha(0f); // invisible "hole"
        } else {
            holder.tileNumber.setText(String.valueOf(tile.getValue()));
            holder.tileNumber.setAlpha(1f);
        }

        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (listener != null && adapterPos != RecyclerView.NO_POSITION) {
                listener.onTileClick(adapterPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tiles.size();
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        TextView tileNumber;

        TileViewHolder(@NonNull View itemView) {
            super(itemView);
            tileNumber = itemView.findViewById(R.id.tileNumber);
        }
    }
}